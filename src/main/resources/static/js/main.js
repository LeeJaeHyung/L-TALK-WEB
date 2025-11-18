// ===== DOM =====
const $list = document.getElementById('list');
const $state = document.getElementById('state');
const $q = document.getElementById('q');
const $btnRefresh = document.getElementById('btnRefresh');
const $wsState = document.getElementById('wsState');

// ===== STATE =====
let ROOMS = [];
// roomId → STOMP subscription
const SUBS = new Map();

// ===== Utils =====
const esc = (s) => String(s ?? '')
    .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
    .replaceAll('"','&quot;').replaceAll("'",'&#39;');

const lastChat = (room) => {
    if (!room || !Array.isArray(room.chats) || room.chats.length === 0) return null;
    return room.chats[room.chats.length-1];
};

const senderName = (room, senderId) => {
    if (!room || !Array.isArray(room.members)) return '알 수 없음';
    // 서버 DTO 예시상: chats[].senderId == members[].id (채팅방 멤버 PK)
    const m = room.members.find(m => String(m.id) === String(senderId));
    return m?.username ?? '알 수 없음';
};

const initials = (name) => {
    const s = String(name||'').trim();
    if (!s) return 'U';
    const parts = s.split(/\s+/);
    if (parts.length >= 2) return (parts[0][0]+parts[1][0]).toUpperCase();
    return parts[0][0].toUpperCase();
};

const fmtRelative = (iso) => {
    if (!iso) return '';
    const d = new Date(String(iso).replace(' ','T'));
    if (isNaN(d)) return esc(iso);
    const now = new Date();
    const diff = (now - d) / 1000;
    if (diff < 60) return '방금 전';
    if (diff < 3600) return Math.floor(diff/60)+'분 전';
    if (diff < 86400) return Math.floor(diff/3600)+'시간 전';
    const dayDiff = Math.floor(diff/86400);
    if (dayDiff === 1) return '어제';
    if (dayDiff === 2) return '그제';
    const y = d.getFullYear(), m = String(d.getMonth()+1).padStart(2,'0'), dd = String(d.getDate()).padStart(2,'0');
    return `${y}-${m}-${dd}`;
};

const sortByLastMessageDesc = (rooms) => {
    return [...rooms].sort((a,b) => {
        const la = lastChat(a)?.createdAt;
        const lb = lastChat(b)?.createdAt;
        const da = la ? new Date(la) : new Date(0);
        const db = lb ? new Date(lb) : new Date(0);
        return db - da;
    });
};

// ===== Render =====
const render = (rooms) => {
    if (!rooms || rooms.length === 0) {
        $list.hidden = true;
        $state.className = 'empty';
        $state.innerHTML = '참여 중인 채팅방이 없습니다.';
        return;
    }
    const html = rooms.map((r) => {
        const last = lastChat(r);
        const lastSender = last ? senderName(r, last.senderId) : null;
        const lastMsg = last ? last.message : '메시지가 없습니다';
        const lastTime = last ? fmtRelative(last.createdAt) : '';
        const names = (r.members||[]).map(m => m.username).slice(0,3);
        const avatarHtml = names.map((n,idx) =>
            `<div class="avatar" style="z-index:${10-idx}">${esc(initials(n))}</div>`
        ).join('');
        const chips = names.map(n => `<span class="chip">${esc(n)}</span>`).join('');
        const more = (r.participantCount ?? (r.members?.length||0)) - names.length;
        const moreChip = more > 0 ? `<span class="chip">+${more}</span>` : '';

        return `
      <div class="row room-item"
           role="button" tabindex="0"
           data-room-id="${esc(r.id)}"
           data-room-name="${esc(r.name || '이름 없는 방')}"
           aria-label="채팅방 ${esc(r.name)} 열기">
        <div class="avatars">${avatarHtml}</div>
        <div class="body">
          <div class="name">
            <span>${esc(r.name || '이름 없는 방')}</span>
            <span class="badge">${esc(r.type || 'ROOM')}</span>
            <span class="badge" title="참여자 수">${(r.participantCount ?? (r.members?.length||0))}명</span>
          </div>
          <div class="meta">${chips}${moreChip}</div>
          <div class="lastline">
            ${ last ? `<strong>${esc(lastSender)}:</strong>` : `<strong>—</strong>`}
            <span class="lastmsg">${esc(lastMsg)}</span>
          </div>
        </div>
        <div class="time">${esc(lastTime)}</div>
      </div>`;
    }).join('');

    $state.innerHTML = '';
    $state.className = '';
    $list.hidden = false;
    $list.innerHTML = html;

    // (수정 2) 페이지 이동 제거: 더블클릭으로 모달을 엽니다. 별도 click 이동 핸들러 없음.
};

const setLoading = () => {
    $list.hidden = true;
    $state.className = 'loading';
    $state.innerHTML = `
    <div class="skeleton"></div>
    <div class="skeleton"></div>
    <div class="skeleton"></div>`;
};

const setError = (msg) => {
    $list.hidden = true;
    $state.className = 'error';
    $state.innerHTML = `<strong>불러오기 실패</strong><div style="margin-top:8px">${esc(msg||'잠시 후 다시 시도해주세요.')}</div>`;
};

// ===== REST: /chatrooms =====
const load = async () => {
    setLoading();
    try{
        const res = await fetch('/chatrooms', { method:'GET' });
        if(!res.ok){
            const t = await res.text().catch(()=> '');
            throw new Error(`${res.status} ${res.statusText} ${t}`);
        }
        const data = await res.json();
        ROOMS = Array.isArray(data) ? sortByLastMessageDesc(data) : [];
        render(ROOMS);

        // 목록이 준비되면 STOMP 연결 및 전체 방 구독
        ensureConnectedThenSyncSubscriptions();
    }catch(err){
        setError(err.message);
    }
};

// ===== Search =====
const applyFilter = () => {
    const term = $q.value.trim().toLowerCase();
    if(!term){ render(ROOMS); return; }
    const filtered = ROOMS.filter(r => {
        const inName = String(r.name||'').toLowerCase().includes(term);
        const members = (r.members||[]).map(m => String(m.username||'').toLowerCase()).join(' ');
        const last = lastChat(r)?.message || '';
        return inName || members.includes(term) || String(last).toLowerCase().includes(term);
    });
    render(filtered);
};
$q.addEventListener('input', applyFilter);
$btnRefresh.addEventListener('click', async () => {
    await load();
    // 새로 로딩한 후 구독 목록도 동기화 (신규 방 추가, 빠진 방 해제)
    syncSubscriptions();
});

// ===== STOMP / SockJS =====
let client = null;
let connecting = false;
const wsBadge = (text, color='#16a34a') => {
    if (!$wsState) return;
    $wsState.textContent = `• ${text}`;
    $wsState.style.color = color;
};

// SockJS endpoint는 서버 설정과 일치해야 함: WebSocketConfig.addEndpoint("/ws")
const sockFactory = () => new SockJS('/ws', null, {
    transports: ['xhr-streaming', 'xhr-polling', 'websocket'],
    transportOptions: {
        'xhr-streaming': { withCredentials: true },
        'xhr-polling':   { withCredentials: true }
    }
});

const createClient = () => new StompJs.Client({
    webSocketFactory: sockFactory,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
    onConnect: () => {
        wsBadge('연결됨');
        // 재연결 시에도 구독을 복구
        syncSubscriptions(true);
    },
    onStompError: (frame) => {
        console.warn('STOMP error:', frame.headers?.message, frame.body);
        wsBadge('오류', '#ef4444');
    },
    onWebSocketClose: (evt) => {
        console.warn('WS closed', evt?.code);
        wsBadge('끊김', '#ef4444');
        connecting = false;
        // 연결이 완전히 끊겼을 때 구독 객체는 모두 무효화 됨 → Map만 비워두고 재연결 시 재구독
        SUBS.clear();
    },
    onWebSocketError: () => {
        console.warn('WS error');
        wsBadge('오류', '#ef4444');
        connecting = false;
        SUBS.clear();
    }
});

const ensureConnectedThenSyncSubscriptions = () => {
    if (client?.active) {
        syncSubscriptions();
        return;
    }
    if (connecting) return;

    client = createClient();
    connecting = true;
    wsBadge('연결 중…', '#f59e0b');

    // 최초 연결 완료 시
    client.onConnect = () => {
        connecting = false;
        wsBadge('연결됨');
        syncSubscriptions(true);
    };
    client.activate();
};

// 구독 경로
const topicOf = (roomId) => `/topic/chatrooms/${roomId}/chats`;

// 방 하나 구독
const subscribeRoom = (roomId) => {
    if (!client?.active) return;
    if (SUBS.has(roomId)) return; // 이미 구독 중
    const topic = topicOf(roomId);

    const sub = client.subscribe(topic, (frame) => {
        // 서버 ChatController.publishToRoom() → ChatDto { id, senderId, chatRoomId, message, createdAt }
        let dto;
        try { dto = JSON.parse(frame.body); }
        catch { dto = { message: frame.body }; }

        const rid = dto.chatRoomId ?? roomId;
        // 수신 메시지로 해당 방의 마지막 메시지/시간 갱신
        applyIncomingMessage(rid, dto);
    });

    SUBS.set(roomId, sub);
};

// 구독 해제
const unsubscribeRoom = (roomId) => {
    const s = SUBS.get(roomId);
    if (s) {
        try { s.unsubscribe(); } catch {}
        SUBS.delete(roomId);
    }
};

// 현재 ROOMS와 구독 목록을 동기화
// forceResubscribe=true이면 재연결 이후 모든 구독 강제 재생성
const syncSubscriptions = (forceResubscribe = false) => {
    if (!client?.active) return;

    const currentIds = new Set(ROOMS.map(r => String(r.id)));

    // 빠진 방 구독 해제
    for (const [rid, sub] of SUBS.entries()) {
        if (!currentIds.has(String(rid)) || forceResubscribe) {
            try { sub.unsubscribe(); } catch {}
            SUBS.delete(rid);
        }
    }
    // 존재하는 방은 모두 구독
    for (const r of ROOMS) {
        subscribeRoom(r.id);
    }
};

// 수신 메시지를 ROOMS에 반영하고 최신순 재정렬 → 화면 갱신
const applyIncomingMessage = (roomId, dto) => {
    const idx = ROOMS.findIndex(r => String(r.id) === String(roomId));
    if (idx < 0) return; // 목록에 없는 방이면 무시 (또는 추가 로직)
    const room = ROOMS[idx];

    const createdAt = dto.createdAt || new Date().toISOString();
    const senderId = dto.senderId ?? null;
    const msg = {
        id: dto.id ?? null,
        senderId: senderId,
        chatRoomId: roomId,
        message: dto.message ?? '',
        createdAt
    };

    if (!Array.isArray(room.chats)) room.chats = [];
    room.chats.push(msg);

    // 최신순으로 재정렬
    ROOMS = sortByLastMessageDesc(ROOMS);

    // 검색어가 있으면 현재 필터 뷰도 갱신
    const term = $q.value.trim().toLowerCase();
    if (term) applyFilter();
    else render(ROOMS);
};

// ===== Boot =====
load();

// =====================
// 채팅 모달 상태/DOM
// =====================
const $chatModal = document.getElementById('chatModal');
const $chatModalClose = document.getElementById('chatModalClose');
const $chatModalRoomName = document.getElementById('chatModalRoomName');
const $chatScroll = document.getElementById('chatScroll');
const $chatList = document.getElementById('chatList');
const $chatLoadingTop = document.getElementById('chatLoadingTop');

// 모달 내부 상태 (커서 기반)
const CHAT_STATE = {
    roomId: null,
    roomName: null,
    items: [],            // 화면 표시용(오름차순: 과거 -> 최신)
    hasNext: true,
    nextCursorAt: null,
    nextCursorId: null,
    loadingTop: false,
    initialized: false
};

const fmtLocal = (v) => {
    if (!v) return '';
    // 서버에서 ISO 형식이라고 가정. 'YYYY-MM-DDTHH:mm:ss' 형태
    const d = new Date(String(v).replace(' ', 'T'));
    if (Number.isNaN(d.getTime())) return String(v);
    // 사용자가 한국이라 가정 (Asia/Seoul)
    return d.toLocaleString('ko-KR', { hour12: false });
};

// =====================
// 모달 열고/닫기
// =====================
function openChatModal(roomId, roomName) {
    // 상태 초기화
    CHAT_STATE.roomId = roomId;
    CHAT_STATE.roomName = roomName || `방 #${roomId}`;
    CHAT_STATE.items = [];
    CHAT_STATE.hasNext = true;
    CHAT_STATE.nextCursorAt = null;
    CHAT_STATE.nextCursorId = null;
    CHAT_STATE.loadingTop = false;
    CHAT_STATE.initialized = false;

    $chatModalRoomName.textContent = CHAT_STATE.roomName;
    $chatList.innerHTML = '';

    // 모달 표시
    $chatModal.hidden = false;

    // 최초 로딩
    loadInitialChats().catch(console.error);
}

function closeChatModal() {
    $chatModal.hidden = true;
    // 필요 시 구독 해제/정리 로직 추가 가능
}

$chatModalClose?.addEventListener('click', closeChatModal);
// 백드롭 클릭 닫기
$chatModal.addEventListener('click', (e) => {
    if (e.target?.dataset?.close) {
        closeChatModal();
    }
});

// =====================
// 데이터 호출
// =====================
// GET /chatrooms/{chatRoomId}/chats?size=50 [&cursorAt=...&cursorId=...]
async function fetchChatSlice({ roomId, cursorAt, cursorId, size = 50 }) {
    const url = new URL(`/chatrooms/${roomId}/chats`, location.origin);
    url.searchParams.set('size', String(size));
    if (cursorAt) url.searchParams.set('cursorAt', cursorAt);
    if (cursorId) url.searchParams.set('cursorId', String(cursorId));

    const res = await fetch(url.toString(), { headers: { 'Accept': 'application/json' } });
    if (!res.ok) throw new Error(`채팅 조회 실패: ${res.status}`);
    return await res.json(); // { content, hasNext, nextCursorAt, nextCursorId }
}

// 최초 로딩: 최신 50개 → 화면에는 오름차순으로 그리고 맨 아래로 스크롤
async function loadInitialChats() {
    const roomId = CHAT_STATE.roomId;
    const data = await fetchChatSlice({ roomId, size: 50 });

    // 서버는 최신 내림차순으로 준다고 가정 → 오름차순으로 뒤집어서 렌더
    const arr = Array.isArray(data.content) ? [...data.content].reverse() : [];
    CHAT_STATE.items = arr;
    CHAT_STATE.hasNext = !!data.hasNext;
    CHAT_STATE.nextCursorAt = data.nextCursorAt || null;
    CHAT_STATE.nextCursorId = data.nextCursorId || null;
    CHAT_STATE.initialized = true;

    renderChatListInitial(arr);
    scrollToBottom($chatScroll);
}

// 과거 더 가져오기: 현재 리스트 맨 위 메시지 기준으로 prepend
async function loadOlderChats() {
    if (!CHAT_STATE.hasNext || CHAT_STATE.loadingTop || !CHAT_STATE.initialized) return;

    CHAT_STATE.loadingTop = true;
    $chatLoadingTop.hidden = false;

    const roomId = CHAT_STATE.roomId;
    const data = await fetchChatSlice({
        roomId,
        cursorAt: CHAT_STATE.nextCursorAt,
        cursorId: CHAT_STATE.nextCursorId,
        size: 50
    });

    const beforeHeight = $chatScroll.scrollHeight;

    // 서버는 최신 내림차순 → 오름차순으로 뒤집어서 '앞쪽'에 붙임
    const newArr = Array.isArray(data.content) ? [...data.content].reverse() : [];
    prependChatItems(newArr);

    CHAT_STATE.hasNext = !!data.hasNext;
    CHAT_STATE.nextCursorAt = data.nextCursorAt || null;
    CHAT_STATE.nextCursorId = data.nextCursorId || null;

    // 스크롤 점프 방지
    const afterHeight = $chatScroll.scrollHeight;
    $chatScroll.scrollTop = afterHeight - beforeHeight;

    $chatLoadingTop.hidden = true;
    CHAT_STATE.loadingTop = false;
}

// =====================
// 렌더링
// =====================
function renderChatListInitial(list) {
    const frag = document.createDocumentFragment();
    for (const c of list) {
        frag.appendChild(renderChatItem(c));
    }
    $chatList.innerHTML = '';
    $chatList.appendChild(frag);
}

function prependChatItems(list) {
    if (!list?.length) return;
    // 상태 먼저 반영
    CHAT_STATE.items = [...list, ...CHAT_STATE.items];

    // DOM prepend
    for (let i = list.length - 1; i >= 0; i--) {
        const node = renderChatItem(list[i]);
        $chatList.insertBefore(node, $chatList.firstChild);
    }
}

function appendChatItem(item) {
    CHAT_STATE.items.push(item);
    $chatList.appendChild(renderChatItem(item));
}

function renderChatItem(c) {
    const div = document.createElement('div');
    div.className = 'chatmsg';
    div.innerHTML = `
    <div class="chatmsg__meta">
      <span class="chatmsg__nick">${esc(c.senderNickname ?? '')}</span>
      <span class="chatmsg__time">${fmtLocal(c.createdAt)}</span>
    </div>
    <div class="chatmsg__text">${esc(c.message ?? '')}</div>
  `;
    return div;
}

function scrollToBottom(scroller) {
    scroller.scrollTop = scroller.scrollHeight;
}

function isAtTop(scroller) {
    return scroller.scrollTop <= 0;
}

// =====================
// 스크롤 이벤트 (위 끝에서 과거 로딩)
// =====================
$chatScroll.addEventListener('scroll', () => {
    if (isAtTop($chatScroll)) {
        loadOlderChats().catch(console.error);
    }
});

// =====================
// 목록 더블클릭 → 모달 열기
// =====================
// 리스트 전체에 위임 (기존 #list를 사용)
document.getElementById('list')?.addEventListener('dblclick', (e) => {
    const item = e.target.closest('.room-item');
    if (!item) return;

    // data-* 속성에서 roomId/name 추출 (렌더 시 세팅되어 있어야 함)
    const roomId = item.dataset.roomId || item.getAttribute('data-room-id');
    const roomName = item.dataset.roomName || item.getAttribute('data-room-name');
    if (!roomId) return;

    openChatModal(Number(roomId), roomName);
});
