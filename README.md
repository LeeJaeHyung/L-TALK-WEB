# LTALK 웹 채팅 프로젝트

LTALK은 Spring Boot와 Java 기반으로 개발한 실시간 채팅 웹 애플리케이션입니다.  
사용자는 친구를 추가하고 채팅방을 생성하며, 실시간 채팅을 주고받을 수 있습니다.

---

## 🏗 프로젝트 구조

```
web_project/
├── LTalkWebApplication.java
├── global/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── exception/
│   └── service/
├── member/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
├── friend/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
├── chat/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
├── chatroom/
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
```

---

## ⚙ 기술 스택

- **Java 17**
- **Spring Boot**
- **Spring Data JPA**
- **Redis (로그인 세션 관리)**
- **Gradle (Groovy DSL)**

---

## 🚀 주요 기능

✅ **회원**
- 회원가입 / 로그인 / 로그아웃
- 닉네임, 이메일, 아이디 중복 검사

✅ **친구**
- 친구 요청 / 수락 / 삭제
- 친구 목록 조회

✅ **채팅방**
- 채팅방 생성 (그룹, 개인)
- 채팅방 참가자 관리

✅ **채팅**
- 실시간 채팅 메시지 처리

✅ **인증/보안**
- Redis 기반 로그인 토큰 관리
- `@LoginMember`를 통한 인증 멤버 처리
- 인터셉터 기반 인증 체크

---

## 📝 실행 방법

1️⃣ Gradle 빌드  
```bash
./gradlew build
```

2️⃣ 애플리케이션 실행  
```bash
./gradlew bootRun
```

---

# 📁 API 엔드포인트 요약

### 🔹 HomeController
| 메서드 | 경로 | 파라미터 | 반환 |
|--------|------|-----------|--------|
| Get | `/` | `` | `"home"` |
| Get | `/home` | `` | `"home"` |

### 🔹 MemberController
| 메서드 | 경로 | 파라미터 | 반환 |
|--------|------|-----------|--------|
| Get | `/login` | `` | `"login"` |
| Post | `/login` | `@ModelAttribute LoginRequest request, HttpServletResponse response` | `ResponseEntity.ok(new LoginResponse("로그인 성공", result.getToken(), result.getLoginMember()))` |
| Get | `/sign-up` | `` | `"sign-up"` |
| Post | `/sign-up` | `@Valid @ModelAttribute SignUpRequest request, BindingResult bindingResult` | `"redirect:/login?message=" + URLEncoder.encode("회원가입이 완료되었습니다!", StandardCharsets.UTF_8)` |
| Delete | `/logout` | `@LoginMember Member member, HttpServletRequest, HttpServletResponse` | `"로그아웃 성공"` |
| Get | `/users/me` | `HttpServletRequest request` | `ResponseEntity.ok((LoginMemberDto)request.getAttribute("member"))` |
| Get | `/users/check-username` | `@Valid @ModelAttribute UsernameCheckRequest request` | `ResponseEntity.ok(response)` |
| Get | `/users/check-email` | `@Valid @ModelAttribute EmailCheckRequest request` | `ResponseEntity.ok(response)` |
| Get | `/users/check-nickname` | `@Valid @ModelAttribute NicknameCheckRequest request` | `ResponseEntity.ok(response)` |
| Patch | `/users/me` | `@LoginMember Member member, @RequestBody MemberPatchRequest memberPatchRequest` | `ResponseEntity.ok(updateMember)` |
| Delete | `/users/me` | `@LoginMember Member member, HttpServletRequest request, HttpServletResponse response` | `ResponseEntity.ok("정상적으로 회원 정보가 지워졌습니다.")` |

### 🔹 ChatController
| 메서드 | 경로 | 파라미터 | 반환 |
|--------|------|-----------|--------|
| Post | `/{chatRoomId}/chats` | `HttpServletRequest request, @PathVariable Long chatRoomId` | `ResponseEntity.status(HttpStatus.CREATED).build()` |

### 🔹 FriendController
| 메서드 | 경로 | 파라미터 | 반환 |
|--------|------|-----------|--------|
| Get | `/` | `HttpServletRequest request` | `ResponseEntity.ok(friendService.getFriendList((LoginMemberDto)request.getAttribute("member")))` |
| Get | `/requests` | `HttpServletRequest request` | `ResponseEntity.ok(friendService.getRequestFriendList(((LoginMemberDto)request.getAttribute("member")).getId()))` |
| Post | `/` | `HttpServletRequest request, @RequestBody FriendRequest friendRequest` | `ResponseEntity.ok(friendService.requestFriend(((LoginMemberDto)request.getAttribute("member")).getId(), friendRequest.getToMemberId()))` |
| Patch | `/{friendId}/accept` | `@LoginMember Member member, @PathVariable Long friendId` | `ResponseEntity.ok(friendList)` |
| Delete | `/{friendId}` | `@LoginMember Member member, @PathVariable Long friendId` | `ResponseEntity.ok(friendList)` |



이 프로젝트는 개발 중이며, 실시간 WebSocket 연동 및 UI가 추가될 예정입니다.  
