const CART_KEY = "cart";
const COMPANY_SEQ = 1; // 하드코딩된 단일 회사

function getCart() {
    return JSON.parse(localStorage.getItem(CART_KEY) || "[]");
}

function saveCart(cart) {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
}

function addToCart(productId, name, price) {
    const cart = getCart();
    const line = cart.find(l => l.productId === productId);
    if (line) {
        line.quantity += 1;
    } else {
        cart.push({ productId, name, price, quantity: 1 });
    }
    saveCart(cart);
}

function removeFromCart(productId) {
    saveCart(getCart().filter(l => l.productId !== productId));
}

function clearCart() {
    localStorage.removeItem(CART_KEY);
}

function cartTotal(cart) {
    return cart.reduce((sum, l) => sum + l.price * l.quantity, 0);
}

const POLL_INITIAL_DELAY_MS = 2000;
const POLL_MAX_DELAY_MS = 15000;
const POLL_MAX_DURATION_MS = 120000;
let pollStartedAt = null;

function statusClass(status) {
    return "status-badge status-" + status;
}

function showResult(result) {
    const box = document.getElementById("result");
    box.style.display = "block";
    box.innerHTML = `
        <p>주문 상태: <span class="${statusClass(result.orderStatus)}">${result.orderStatus}</span></p>
        <p>결제 상태: <span class="${statusClass(result.paymentStatus)}">${result.paymentStatus}</span></p>
        <p class="muted">tid: ${result.tid ?? "-"}</p>
        <p class="muted">${result.message ?? ""}</p>
    `;
}

function showNotFoundNotice() {
    const box = document.getElementById("result");
    box.style.display = "block";
    box.innerHTML = `<p class="muted">결제 정보를 찾을 수 없습니다.</p>`;
}

function showPollingTimeoutNotice() {
    document.getElementById("result").innerHTML += `<p class="muted">확인이 지연되고 있습니다. 잠시 후 새로고침해 주세요.</p>`;
}

function fetchPaymentStatus(paymentKey) {
    return fetch(`/api/payments/${paymentKey}`).then(res => (res.ok ? res.json() : null));
}

function startPolling(paymentKey) {
    pollStartedAt = Date.now();
    schedulePoll(paymentKey, POLL_INITIAL_DELAY_MS);
}

function schedulePoll(paymentKey, delay) {
    setTimeout(() => pollOnce(paymentKey, delay), delay);
}

function pollOnce(paymentKey, currentDelay) {
    fetchPaymentStatus(paymentKey).then(result => {
        if (!result) {
            showNotFoundNotice();
            return;
        }
        showResult(result);
        if (result.paymentStatus !== "PENDING") {
            return;
        }
        if (Date.now() - pollStartedAt >= POLL_MAX_DURATION_MS) {
            showPollingTimeoutNotice();
            return;
        }
        schedulePoll(paymentKey, Math.min(currentDelay * 2, POLL_MAX_DELAY_MS));
    });
}
