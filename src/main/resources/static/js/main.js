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
      <div class="row" role="button" tabindex="0" data-id="${esc(r.id)}" aria-label="채팅방 ${esc(r.name)} 열기">
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

    // 클릭 이동
    $list.querySelectorAll('.row').forEach(row => {
        row.addEventListener('click', () => {
            const id = row.getAttribute('data-id');
            window.location.href = `/chat/${id}`;
        });
        row.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                row.click();
            }
        });
    });
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
