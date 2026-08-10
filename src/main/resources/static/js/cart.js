const CART_KEY = "cart";
const ORDER_KEY_KEY = "orderKey";
const COMPANY_SEQ = 1; // 하드코딩된 단일 회사

function getCart() {
    return JSON.parse(localStorage.getItem(CART_KEY) || "[]");
}

function saveCart(cart) {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
    invalidateOrderKey();
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
    invalidateOrderKey();
}

function cartTotal(cart) {
    return cart.reduce((sum, l) => sum + l.price * l.quantity, 0);
}

// 서버가 응답을 주지 않으면 fetch가 무한정 매달려 있을 수 있어, 명시적으로 타임아웃을 건다.
function fetchWithTimeout(url, options, timeoutMs) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    return fetch(url, { ...options, signal: controller.signal }).finally(() => clearTimeout(timer));
}

// 장바구니 내용이 바뀌지 않는 한 같은 orderKey를 재사용한다(재시도 시 같은 주문으로 취급되도록).
function getOrderKey() {
    let orderKey = localStorage.getItem(ORDER_KEY_KEY);
    if (!orderKey) {
        orderKey = crypto.randomUUID();
        localStorage.setItem(ORDER_KEY_KEY, orderKey);
    }
    return orderKey;
}

function invalidateOrderKey() {
    localStorage.removeItem(ORDER_KEY_KEY);
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

const POLL_FETCH_TIMEOUT_MS = 10000;

function fetchPaymentStatus(paymentKey) {
    return fetchWithTimeout(`/api/payments/${paymentKey}`, {}, POLL_FETCH_TIMEOUT_MS)
        .then(res => (res.ok ? res.json() : null));
}

function startPolling(paymentKey) {
    pollStartedAt = Date.now();
    schedulePoll(paymentKey, POLL_INITIAL_DELAY_MS);
}

function schedulePoll(paymentKey, delay) {
    setTimeout(() => pollOnce(paymentKey, delay), delay);
}

// 다음 폴링을 이어가거나, 총 폴링 시간이 다 됐으면 멈춘다. 정상 응답의 PENDING과
// 타임아웃/네트워크 오류를 동일하게 "아직 모름"으로 취급해 같은 방식으로 재시도한다.
function continuePollingOrTimeout(paymentKey, currentDelay) {
    if (Date.now() - pollStartedAt >= POLL_MAX_DURATION_MS) {
        showPollingTimeoutNotice();
        return;
    }
    schedulePoll(paymentKey, Math.min(currentDelay * 2, POLL_MAX_DELAY_MS));
}

function pollOnce(paymentKey, currentDelay) {
    fetchPaymentStatus(paymentKey)
        .then(result => {
            if (!result) {
                showNotFoundNotice();
                return;
            }
            showResult(result);
            if (result.paymentStatus !== "PENDING") {
                return;
            }
            continuePollingOrTimeout(paymentKey, currentDelay);
        })
        .catch(() => continuePollingOrTimeout(paymentKey, currentDelay));
}
