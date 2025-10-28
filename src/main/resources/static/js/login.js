document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const res = await fetch("/login", {
        method: "POST",
        body: formData
    });
    if (res.ok) {
        window.location.href = "/home";
    } else {
        alert("로그인 실패");
    }
});