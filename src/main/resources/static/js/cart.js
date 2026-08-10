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
