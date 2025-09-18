'use strict';

/*
  LTALK 채팅방 테스트 스크립트

  이 파일은 다음 백엔드 API/프로토콜과 연동됩니다.
  - REST
    1) GET /chatrooms → List<ChatRoomDto>
       - 각 항목에 최근 채팅 1건이 ChatDto 형태로 포함될 수 있음(senderId만 존재)
    2) GET /chatrooms/{chatRoomId} → ChatRoomViewDto
       - 해당 방의 모든 채팅이 ChatViewDto 형태로 반환됨(senderNickname 포함)

  - STOMP over SockJS
    - 구독 토픽: /topic/chatrooms/{roomId}/chats
    - 발행(서버): ChatController.publishToRoom(...)에서 ChatDto(혹은 ViewDto) 직렬화하여 동일 토픽으로 전송
    - 주의: STOMP frame body는 문자열. JSON.parse가 필요함.

  로컬 상태 관리
  - roomsData: REST로 받은 채팅방 메타/최근 메시지(목록/상세 반영)
  - roomMessages: Map(roomId → 정규화된 메시지 배열). 메시지는 모두 {id, chatRoomId, message, createdAt, senderId, senderNickname} 형태로 보관
  - subscriptions: STOMP 구독 핸들

  정규화/표시 규칙
  - REST(목록): ChatDto(senderId)만 온 경우가 있어 senderNickname 없음
  - REST(상세): ChatViewDto(senderNickname) 포함
  - STOMP: 서버 전송 형식에 따라 ChatDto 또는 ChatViewDto가 올 수 있으므로, 가능하면 senderNickname을 우선 사용하고 없으면 senderId로 대체 표기
*/

// ---------- 유틸 ----------
function toKST(dateStr) {
    if (!dateStr) return '';
    let s = String(dateStr).trim();
    if (s.includes(' ') && !s.includes('T')) s = s.replace(' ', 'T');
    s = s.replace(/(\.\d{3})\d+$/, '$1');
    const d = new Date(s);
    if (isNaN(d.getTime())) return dateStr;
    return new Intl.DateTimeFormat('ko-KR', {
        dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Seoul'
    }).format(d);
}
function esc(s){
    return String(s ?? '')
        .replaceAll('&','&amp;').replaceAll('<','&lt;')
        .replaceAll('>','&gt;').replaceAll('"','&quot;')
        .replaceAll("'","&#39;");
}

// ---------- 전역 상태 ----------
let roomsData = [];                 // REST 원본
let subscriptions = new Map();      // roomId(string) -> stompSubscription
let client = null;
let connecting = false;

// 구독 요청 큐(연결 전 요청 임시 보관)
const pendingSubs = new Set();      // Set<string roomId>

// 모달 상태
let currentModalRoomId = null;
const roomMessages = new Map();     // roomId -> [{message, senderId, createdAt}...]

// STOMP 경로: 서버/클라 모두 이 경로로 통일해야 수신 가능
const topicOf = (rid) => `/topic/chatrooms/${rid}/chats`;
const appOf   = (rid) => `/app/chatrooms/${rid}/chats`;

// ---------- 리스트 렌더 ----------
// 채팅방 목록 카드 렌더링
function renderRooms(rooms, filter='') {
    const list = document.getElementById('list');
    list.innerHTML = '';
    const q = filter.trim().toLowerCase();

    const filtered = rooms.filter(r => {
        const name = (r.name||'').toLowerCase();
        const members = (r.members||[]).map(m=>m.username||'').join(' ').toLowerCase();
        const last = (r.chats&&r.chats[0]? r.chats[0].message : '').toLowerCase();
        return !q || name.includes(q) || members.includes(q) || last.includes(q);
    });

    if (filtered.length === 0) {
        list.innerHTML = '<div class="muted">표시할 채팅방이 없어요.</div>';
        return;
    }

    for (const room of filtered) {
        const type = room.type ?? 'UNKNOWN';
        const members = room.members?.map(m => esc(m.username)).join(', ') || '-';
        const last = room.chats && room.chats[0] ? room.chats[0] : null;

        const card = document.createElement('div');
        card.className = 'card';
        card.dataset.roomId = room.id;
        card.id = `room-${room.id}`;
        card.title = '더블클릭하면 이 방 채팅 모달이 열립니다';

        card.innerHTML = `
      <h3>${esc(room.name || '(이름 없음)')}</h3>
      <div class="meta">
        <span class="badge">ID: ${room.id}</span>
        <span class="badge">타입: ${esc(type)}</span>
        <span class="badge">인원: ${room.participantCount}</span>
        <span class="badge" id="sub-${room.id}">구독: ${subscriptions.has(String(room.id)) ? 'ON' : (pendingSubs.has(String(room.id)) ? 'PENDING' : 'OFF')}</span>
      </div>
      <div class="members"><strong>멤버</strong>: ${members}</div>
      <div class="last" id="last-${room.id}">
        ${
            last
                ? `<div class="row">
                 <span class="badge">마지막 메시지</span>
                 <span class="muted">${toKST(last.createdAt)}</span>
               </div>
               <div style="margin-top:6px;">${esc(last.message)}</div>`
                : `<div class="muted">아직 메시지가 없어요.</div>`
        }
      </div>
    `;
        list.appendChild(card);
    }
}

// STOMP 수신 또는 버퍼 반영 시 카드/모달 갱신
function updateRoomCardByMessage(msgObj){
    const rid = msgObj?.chatRoomId ?? msgObj?.roomId ?? msgObj?.chatroomId;
    if (!rid) return;

    const lastBox = document.getElementById(`last-${rid}`);
    if (lastBox) {
        lastBox.innerHTML =
            `<div class="row">
         <span class="badge">마지막 메시지</span>
         <span class="muted">${toKST(msgObj.createdAt)}</span>
       </div>
       <div style="margin-top:6px;">${esc(msgObj.message ?? '')}</div>`;
    }

    const subBadge = document.getElementById(`sub-${rid}`);
    if (subBadge) subBadge.textContent = '구독: ON';

    const key = String(rid);
    const arr = roomMessages.get(key) ?? [];
    // 정규화된 형태로 메시지 버퍼에 push (닉네임 우선)
    arr.push({
        message: msgObj.message ?? '',
        senderId: msgObj.senderId,
        senderNickname: msgObj.senderNickname,
        createdAt: msgObj.createdAt
    });
    roomMessages.set(key, arr);

    if (currentModalRoomId && String(currentModalRoomId) === key) {
        appendChatRow(msgObj);
    }
}

// ---------- REST ----------
// 채팅방 목록 조회: GET /chatrooms → List<ChatRoomDto>
async function loadRooms() {
    const status = document.getElementById('status');
    const list   = document.getElementById('list');
    status.hidden = false; list.hidden = true;
    status.className = 'loading';
    status.textContent = '불러오는 중...';
    var startDate = new Date();

    try {
        const res = await fetch('/chatrooms', { method:'GET', headers:{'Accept':'application/json'}, credentials:'include' });
        if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
        const data = await res.json(); // ChatRoomDto[]
        roomsData = Array.isArray(data) ? data : [];

        // 마지막 메시지 버퍼에 심기(목록의 최근 1건은 ChatDto로, 닉네임이 없을 수 있음)
        roomsData.forEach(r => {
            const key = String(r.id);
            if (!roomMessages.has(key)) roomMessages.set(key, []);
            if (r.chats && r.chats[0]) {
                const last = r.chats[0];
                const arr = roomMessages.get(key);
                if (!arr.some(x => x.createdAt === last.createdAt && x.message === last.message)) {
                    arr.push({ message: last.message, senderId: last.senderId, createdAt: last.createdAt });
                }
            }
        });

        renderRooms(roomsData);
        status.hidden = true; list.hidden = false;

        // 검색 연동
        const q = document.getElementById('q');
        q.oninput = () => renderRooms(roomsData, q.value);

        // 연결 상태에 따라: 바로 재구독 또는 큐에 적재
        const allIds = roomsData.map(r => r.id);
        if (client?.active) {
            resubscribeAll();
        } else {
            queueSubscribeAll(allIds);  // ← 연결 안되면 큐
        }
    } catch (e) {
        status.hidden = false; list.hidden = true;
        status.className = 'error';
        status.textContent = `불러오기 실패: ${e.message}`;
    }
    var endDate = new Date();
    console.log((endDate.getTime()-startDate.getTime())+"걸림");
}

// 방 상세(모달): GET /chatrooms/{chatRoomId} → ChatRoomViewDto
async function fetchRoomDetail(roomId) {
    console.log("채팅 데이터 불러오기중");
    var startDate = new Date();
    const res = await fetch(`/chatrooms/${roomId}`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        credentials: 'include'
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    const dto = await res.json(); // ChatRoomViewDto

    const key = String(roomId);
    // 상세의 chats는 ChatViewDto(닉네임 포함) 기준으로 정규화
    const arr = (dto.chats || []).map(c => ({
        message: c.message,
        senderId: c.senderId,
        senderNickname: c.senderNickname,
        createdAt: c.createdAt
    }));
    roomMessages.set(key, arr);

    const idx = roomsData.findIndex(r => String(r.id) === String(roomId));
    if (idx >= 0) roomsData[idx] = dto;
    console.log((new Date().getTime()-startDate.getTime())+"ms 걸림");
    return dto;
}

// ---------- STOMP ----------
// SockJS 팩토리: /ws 엔드포인트는 서버 WebSocketConfig와 일치해야 함
const sockFactory = () => new SockJS('/ws', null, {
    transports: ['xhr-streaming','xhr-polling','websocket'],
    transportOptions: {
        'xhr-streaming': { withCredentials: true },
        'xhr-polling':   { withCredentials: true }
    }
});

// STOMP 클라이언트 생성: 연결/오류/종료 핸들링 및 재구독
function createClient(){
    return new StompJs.Client({
        webSocketFactory: sockFactory,
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: () => {},
        onConnect: () => {
            // 연결되면: 1) 현재 목록 전체 구독, 2) 큐에 쌓인 구독 일괄 처리
            const ids = roomsData.map(r => r.id);
            subscribeAllRooms(ids);
            drainPendingSubscriptions();
            const btn = document.getElementById('connect');
            if (btn) btn.textContent = '재연결';
        },
        onStompError: (f) => console.warn('STOMP error', f.headers?.message, f.body),
        onWebSocketClose: () => {
            // 로컬 상태 정리
            for (const sub of subscriptions.values()) { try { sub.unsubscribe(); } catch{} }
            subscriptions.clear();
            // 뷰 업데이트 (PENDING/OFF 보이도록)
            renderRooms(roomsData, document.getElementById('q')?.value || '');
            const btn = document.getElementById('connect');
            if (btn) btn.textContent = '웹소켓 연결';
            connecting = false;
        },
        onWebSocketError: () => { connecting = false; }
    });
}

// 연결 보장: 중복 연결 방지, onConnect는 createClient 설정 사용
function ensureConnected(){
    if (client?.active || connecting) return;
    client = createClient();
    connecting = true;
    // onConnect는 createClient 내부에 정의되어 있음
    client.activate();
}

// --- 구독 큐 유틸 ---
function queueSubscribe(roomId){
    pendingSubs.add(String(roomId));
    // 배지에 PENDING 표시
    const badge = document.getElementById(`sub-${roomId}`);
    if (badge && !subscriptions.has(String(roomId))) badge.textContent = '구독: PENDING';
    ensureConnected();
}
function queueSubscribeAll(roomIds){
    roomIds.forEach(id => pendingSubs.add(String(id)));
    // 리스트 재렌더해서 PENDING 뱃지 반영
    renderRooms(roomsData, document.getElementById('q')?.value || '');
    ensureConnected();
}
function drainPendingSubscriptions(){
    if (!client?.active || pendingSubs.size === 0) return;
    const ids = [...pendingSubs];
    pendingSubs.clear();
    subscribeAllRooms(ids);
}

// --- 실제 구독 ---
// 특정 방 1회 구독: JSON.parse 성공 시 객체로, 실패 시 평문 메시지로 처리
function subscribeRoomOnce(rid){
    const key = String(rid);
    if (!client?.active) { queueSubscribe(rid); return; } // ← 연결 전이면 큐
    if (subscriptions.has(key)) return; // 이미 구독 중
    const sub = client.subscribe(topicOf(rid), (frame) => {
        const body = frame?.body;
        try {
            const obj = JSON.parse(body); // 서버가 DTO(Object)를 보낸 경우
            updateRoomCardByMessage(obj);
        } catch {
            // 서버가 순수 문자열을 보낸 경우(권장 X): 평문으로 표시
            updateRoomCardByMessage({ chatRoomId: rid, message: body, createdAt: new Date().toISOString() });
        }
    });
    subscriptions.set(key, sub);

    const badge = document.getElementById(`sub-${rid}`);
    if (badge) badge.textContent = '구독: ON';

    if (currentModalRoomId && String(currentModalRoomId) === key) {
        const subState = document.getElementById('modal-substate');
        if (subState) subState.textContent = '구독: ON';
    }
}

function subscribeAllRooms(roomIds){
    if (!client?.active) { queueSubscribeAll(roomIds); return; } // ← 연결 전이면 큐
    const keep = new Set(roomIds.map(String));
    for (const [rid, sub] of subscriptions.entries()) {
        if (!keep.has(rid)) { try { sub.unsubscribe(); } catch{} subscriptions.delete(rid); }
    }
    roomIds.forEach((rid, i) => {
        setTimeout(() => subscribeRoomOnce(rid), i * 50); // 급폭주 방지
    });
}

function resubscribeAll(){
    subscribeAllRooms(roomsData.map(r => r.id));
}

// 메시지 전송(낙관적 반영 없음)
// 메시지 전송: SEND /app/chatrooms/{rid}/chats, 서버에서 저장 후 동일 토픽으로 브로드캐스트
function publishToRoom(rid, text){
    if (!client?.active) {
        alert('웹소켓이 연결되어 있지 않습니다.');
        return;
    }
    client.publish({
        destination: appOf(rid),
        body: JSON.stringify({ message: text })
    });
}

// ---------- 모달 ----------
const $backdrop = document.getElementById('backdrop');
const $closeModal = document.getElementById('closeModal');
const $chatLog = document.getElementById('chatLog');
const $chatInput = document.getElementById('chatInput');
const $sendBtn = document.getElementById('sendBtn');
const $modalStatus = document.getElementById('modalStatus');

function openModalForRoom(roomId){
    currentModalRoomId = roomId;

    const room = roomsData.find(r => String(r.id) === String(roomId));
    const $title = document.getElementById('modal-title');
    const $sub = document.getElementById('modal-sub');
    const $substate = document.getElementById('modal-substate');

    if ($title) $title.textContent = room?.name || `채팅방 #${roomId}`;
    if ($sub) $sub.textContent = `ID: ${roomId} · 인원: ${room?.participantCount ?? '-'}`;
    if ($substate) $substate.textContent =
        `구독: ${subscriptions.has(String(roomId)) ? 'ON' : (pendingSubs.has(String(roomId)) ? 'PENDING' : 'OFF')}`;

    // 안전망: 연결 전이면 큐에, 연결되어 있으면 즉시 구독
    if (subscriptions.has(String(roomId))) {
        // already ON
    } else if (client?.active) {
        subscribeRoomOnce(roomId);
    } else {
        queueSubscribe(roomId);
    }

    if ($chatLog) $chatLog.innerHTML = '';
    if ($modalStatus) $modalStatus.textContent = '채팅 내역 불러오는 중…';
    if ($backdrop) {
        $backdrop.style.display = 'flex';
        $backdrop.setAttribute('aria-hidden', 'false');
    }

    fetchRoomDetail(roomId)
        .then(dto => {
            if ($title) $title.textContent = dto?.name || `채팅방 #${roomId}`;
            if ($sub) $sub.textContent = `ID: ${roomId} · 인원: ${dto?.participantCount ?? '-'}`;
            renderChatLog(roomId);
            if ($modalStatus) $modalStatus.textContent = '';
            $chatInput?.focus();
        })
        .catch(e => {
            if ($modalStatus) $modalStatus.textContent = `불러오기 실패: ${e.message}`;
        });
}

function closeModal(){
    currentModalRoomId = null;
    if ($backdrop) {
        $backdrop.style.display = 'none';
        $backdrop.setAttribute('aria-hidden', 'true');
    }
    if ($chatLog) $chatLog.innerHTML = '';
    if ($chatInput) $chatInput.value = '';
    if ($modalStatus) $modalStatus.textContent = '';
}

function renderChatLog(roomId){
    const arr = roomMessages.get(String(roomId)) ?? [];
    if ($chatLog) {
        $chatLog.innerHTML = '';
        for (const m of arr.slice(-500)) {
            appendChatRow(m);
        }
        $chatLog.scrollTop = $chatLog.scrollHeight;
    }
}

// 채팅 한 줄 렌더: 닉네임 우선, 없으면 senderId로 대체 표기
function appendChatRow(m){
    if (!$chatLog) return;
    const row = document.createElement('div');
    row.className = 'chat-row';
    const time = toKST(m.createdAt ?? new Date().toISOString());
    const who = (m.senderNickname && m.senderNickname.trim())
        ? m.senderNickname
        : (m.senderId != null ? `sender:${m.senderId}` : '');
    row.innerHTML = `${esc(m.message ?? '')} <span class="chat-time">(${esc(who)}${who ? ' · ' : ''}${time})</span>`;
    $chatLog.appendChild(row);
    $chatLog.scrollTop = $chatLog.scrollHeight;
}

// ---------- 이벤트 바인딩 유틸 ----------
function on(el, evt, handler) { if (el) el.addEventListener(evt, handler); }
const $refresh    = document.getElementById('refresh');
const $connectBtn = document.getElementById('connect');
const $list       = document.getElementById('list');

// ---------- 이벤트 ----------
on($refresh, 'click', loadRooms);
on($connectBtn, 'click', () => {
    ensureConnected();
    // 사용자가 수동 연결 시, 큐에 뭔가 있다면 바로 처리
    drainPendingSubscriptions();
});
on($list, 'dblclick', (e) => {
    const card = e.target.closest('.card');
    if (!card) return;
    const rid = card.dataset.roomId;
    if (!rid) return;
    openModalForRoom(rid);
});
on($closeModal, 'click', closeModal);
on($backdrop, 'click', (e) => { if (e.target === $backdrop) closeModal(); });
on(window, 'keydown', (e) => { if (e.key === 'Escape' && $backdrop?.style.display === 'flex') closeModal(); });

function sendFromModal(){
    if (!currentModalRoomId) return;
    const text = ($chatInput?.value || '').trim();
    if (!text) return;
    publishToRoom(currentModalRoomId, text);
    if ($chatInput) { $chatInput.value = ''; $chatInput.focus(); }
}
on($sendBtn, 'click', sendFromModal);
on($chatInput, 'keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendFromModal();
    }
});

// 초기 진입 (해당 페이지 구조가 있을 때만 동작)
window.addEventListener('load', () => {
    if ($list) {
        loadRooms();        // 1) 목록
        ensureConnected();  // 2) 연결 시도 (성공 시 pendingSubs도 처리됨)
    }
});
