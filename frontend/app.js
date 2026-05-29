const API = 'http://localhost:8080/api';

// ============================================================
// API Client
// ============================================================
const http = {
  async request(method, path, body = null, params = null) {
    const url = new URL(`${API}${path}`);
    if (params) Object.entries(params).forEach(([k, v]) => { if (v != null && v !== '') url.searchParams.set(k, v); });

    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('token');
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(url.toString(), opts);
    const json = res.headers.get('content-type')?.includes('application/json') ? await res.json() : null;

    if (!res.ok) {
      const msg = json?.message || json?.error || `Request failed (${res.status})`;
      throw new ApiError(res.status, msg, json);
    }
    return json;
  },
  get(path, params) { return this.request('GET', path, null, params); },
  post(path, body) { return this.request('POST', path, body); },
  put(path, body) { return this.request('PUT', path, body); },
  patch(path, body) { return this.request('PATCH', path, body); },
  del(path) { return this.request('DELETE', path); },
};

class ApiError extends Error {
  constructor(status, message, data) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

// ============================================================
// State
// ============================================================
const State = {
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  cart: JSON.parse(localStorage.getItem('cart') || '[]'),
  products: [],
  orders: [],
  payments: [],
  currentProduct: null,
  currentPage: 0,
  totalPages: 1,

  get isLoggedIn() { return !!this.user && !!localStorage.getItem('token'); },
  get isAdmin() { return this.user?.role === 'ADMIN'; },

  setAuth(token, user) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    this.user = user;
  },
  clearAuth() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.user = null;
  },
  setCart(cart) {
    this.cart = cart;
    localStorage.setItem('cart', JSON.stringify(cart));
  },
  addToCart(productId, name, price, qty = 1) {
    const existing = this.cart.find(i => i.productId === productId);
    if (existing) { existing.quantity += qty; }
    else { this.cart.push({ productId, name, price, quantity: qty }); }
    this.setCart([...this.cart]);
  },
  removeFromCart(productId) {
    this.setCart(this.cart.filter(i => i.productId !== productId));
  },
  clearCart() { this.setCart([]); },
  get cartTotal() { return this.cart.reduce((s, i) => s + i.price * i.quantity, 0); },
  get cartCount() { return this.cart.reduce((s, i) => s + i.quantity, 0); },
};

// ============================================================
// Router
// ============================================================
const Router = {
  routes: {},
  add(path, handler) { this.routes[path] = handler; },
  navigate(path) { window.location.hash = path; },
  init() {
    window.addEventListener('hashchange', () => this.resolve());
    this.resolve();
  },
  resolve() {
    const hash = window.location.hash.slice(1) || '/';
    const route = Object.keys(this.routes).find(r => {
      if (r === hash) return true;
      if (r.includes(':')) {
        const pattern = new RegExp('^' + r.replace(/:([^/]+)/g, '([^/]+)') + '$');
        return pattern.test(hash);
      }
      return false;
    });

    document.querySelectorAll('.sidebar__link[data-route]').forEach(link => {
      link.classList.toggle('active', link.dataset.route === route || (hash.startsWith('/products/') && route === '/products/:id'));
    });

    if (route) {
      const match = hash.match(new RegExp('^' + route.replace(/:([^/]+)/g, '([^/]+)') + '$'));
      this.routes[route](...(match ? match.slice(1) : []));
    } else {
      this.routes['/']();
    }
  },
};

// ============================================================
// Views
// ============================================================
function render(html) {
  document.getElementById('main-content').innerHTML = html;
}

function formatPrice(p) { return `$${Number(p).toFixed(2)}`; }
function formatDate(d) { return d ? new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'; }

function statusBadge(status) {
  const s = (status || '').toLowerCase();
  return `<span class="badge badge--${s}">${status}</span>`;
}

// Home Page
async function HomeView() {
  const cartHtml = State.cartCount > 0
    ? `<a href="#/checkout" class="btn btn--primary">Checkout (${State.cartCount} items)</a>`
    : '';

  render(`
    <div class="hero">
      <h1>Welcome to SmartShop</h1>
      <p class="subtitle">Browse our curated collection of products</p>
      <div class="hero__actions">
        <a href="#/products" class="btn btn--primary">Browse Catalog</a>
        ${cartHtml}
      </div>
    </div>
    <h2 style="margin-bottom:16px">Featured Products</h2>
    <div class="card-grid" id="featured-grid"></div>
  `);

  try {
    const data = await http.get('/products', { page: 0, size: 6 });
    const products = data.content || [];
    const grid = document.getElementById('featured-grid');
    if (!grid) return;
    if (products.length === 0) {
      grid.innerHTML = '<div class="empty-state"><h3>No products yet</h3><p>Add some products to get started</p></div>';
      return;
    }
    grid.innerHTML = products.map(p => productCard(p)).join('');
  } catch (e) {
    document.getElementById('featured-grid').innerHTML = `<div class="alert alert--error">${e.message}</div>`;
  }
}

function productCard(p) {
  const stockClass = p.quantity <= 5 ? 'product-card__stock--low' : '';
  return `
    <article class="card product-card" onclick="Router.navigate('/products/${p.id}')" tabindex="0" role="button" aria-label="View ${p.name}">
      ${p.imageUrl
        ? `<img class="product-card__img" src="${p.imageUrl}" alt="${p.name}" loading="lazy">`
        : `<div class="product-card__img-placeholder">${getCategoryIcon(p.category)}</div>`}
      <span class="product-card__category">${p.category}</span>
      <h3 class="product-card__name">${p.name}</h3>
      <p class="product-card__desc">${p.description || ''}</p>
      <div class="product-card__footer">
        <span class="product-card__price">${formatPrice(p.price)}</span>
        <span class="product-card__stock ${stockClass}">${p.quantity > 0 ? p.quantity + ' in stock' : 'Out of stock'}</span>
      </div>
    </article>
  `;
}

function getCategoryIcon(cat) {
  const icons = { ELECTRONICS: '💻', CLOTHING: '👕', BOOKS: '📚', HOME: '🏠', SPORTS: '⚽', TOYS: '🧸', FOOD: '🍕', OTHER: '📦' };
  return icons[cat] || '📦';
}

// Products (Catalog)
async function ProductsView() {
  render(`
    <div class="page-header">
      <h1>Catalog</h1>
    </div>
    <div style="display:flex;gap:12px;margin-bottom:20px;flex-wrap:wrap">
      <input type="search" class="form-input" style="max-width:300px" placeholder="Search products..." id="search-input">
      <select class="form-input" style="max-width:200px" id="category-filter">
        <option value="">All Categories</option>
        <option value="ELECTRONICS">Electronics</option>
        <option value="CLOTHING">Clothing</option>
        <option value="BOOKS">Books</option>
        <option value="HOME">Home</option>
        <option value="SPORTS">Sports</option>
        <option value="TOYS">Toys</option>
        <option value="FOOD">Food</option>
        <option value="OTHER">Other</option>
      </select>
    </div>
    <div class="card-grid" id="products-grid"></div>
    <div class="pagination" id="products-pagination"></div>
  `);

  let debounceTimer;
  document.getElementById('search-input').addEventListener('input', (e) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => loadProducts(0, e.target.value, document.getElementById('category-filter').value), 300);
  });
  document.getElementById('category-filter').addEventListener('change', (e) => {
    loadProducts(0, document.getElementById('search-input').value, e.target.value);
  });

  loadProducts(0);
}

async function loadProducts(page = 0, search = '', category = '') {
  const grid = document.getElementById('products-grid');
  if (!grid) return;
  grid.innerHTML = Array(6).fill('<div class="card skeleton skeleton--img" style="height:280px"></div>').join('');

  try {
    const params = { page, size: 12 };
    if (search) params.search = search;
    if (category) params.category = category;

    const data = await http.get('/products', params);
    State.products = data.content || [];
    State.currentPage = data.page;
    State.totalPages = data.totalPages;

    if (State.products.length === 0) {
      grid.innerHTML = '<div class="empty-state"><h3>No products found</h3><p>Try a different search or category</p></div>';
      document.getElementById('products-pagination').innerHTML = '';
      return;
    }

    grid.innerHTML = State.products.map(p => productCard(p)).join('');
    renderPagination('products-pagination', data, (p) => loadProducts(p, search, category));
  } catch (e) {
    grid.innerHTML = `<div class="alert alert--error">${e.message}</div>`;
  }
}

function renderPagination(id, data, callback) {
  const el = document.getElementById(id);
  if (!el || data.totalPages <= 1) { el.innerHTML = ''; return; }

  let html = `<button ${data.page <= 0 ? 'disabled' : ''} onclick="window._pagPrev()">←</button>`;
  for (let i = 0; i < data.totalPages; i++) {
    html += `<button class="${i === data.page ? 'active' : ''}" onclick="window._pagGoto(${i})">${i + 1}</button>`;
  }
  html += `<button ${data.page >= data.totalPages - 1 ? 'disabled' : ''} onclick="window._pagNext()">→</button>`;
  el.innerHTML = html;

  window._pagPrev = () => callback(data.page - 1);
  window._pagNext = () => callback(data.page + 1);
  window._pagGoto = (p) => callback(p);
}

// Product Detail
async function ProductDetailView(id) {
  render('<div style="padding:48px;display:flex;justify-content:center"><div class="spinner"></div></div>');

  try {
    const p = await http.get(`/products/${id}`);
    State.currentProduct = p;

    render(`
      <div style="margin-bottom:20px">
        <a href="#/products" style="color:var(--text-2);text-decoration:none;font-size:0.9rem">← Back to catalog</a>
      </div>
      <div class="product-detail">
        <div class="product-detail__img">
          ${p.imageUrl ? `<img src="${p.imageUrl}" alt="${p.name}" style="width:100%;height:100%;object-fit:cover;border-radius:var(--radius-md)">` : `<span style="font-size:4rem">${getCategoryIcon(p.category)}</span>`}
        </div>
        <div class="product-detail__info">
          <span class="product-card__category">${p.category}</span>
          <h1>${p.name}</h1>
          <div class="product-detail__meta">
            <span>SKU: ${p.sku}</span>
            <span>${p.quantity > 0 ? `${p.quantity} in stock` : 'Out of stock'}</span>
          </div>
          <div class="product-detail__price">${formatPrice(p.price)}</div>
          <p class="product-detail__desc">${p.description || 'No description available.'}</p>
          ${State.isLoggedIn && p.quantity > 0 ? `
            <div style="display:flex;align-items:center;gap:16px;margin-top:8px">
              <div class="qty-control">
                <button onclick="updateDetailQty(-1)" aria-label="Decrease quantity">−</button>
                <input type="number" id="detail-qty" value="1" min="1" max="${p.quantity}" aria-label="Quantity">
                <button onclick="updateDetailQty(1)" aria-label="Increase quantity">+</button>
              </div>
              <button class="btn btn--primary" onclick="addToCartFromDetail()">Add to Cart</button>
            </div>
          ` : !State.isLoggedIn ? `<a href="#/login" class="btn btn--primary" style="margin-top:16px">Login to Purchase</a>` : ''}
          ${!p.active ? '<div class="alert alert--error" style="margin-top:16px">This product is no longer available</div>' : ''}
        </div>
      </div>
    `);
  } catch (e) {
    render(`<div class="alert alert--error">${e.message}</div><a href="#/products" class="btn btn--secondary" style="margin-top:16px">← Back to catalog</a>`);
  }
}

window.updateDetailQty = function (delta) {
  const input = document.getElementById('detail-qty');
  const val = Math.max(1, Math.min(State.currentProduct.quantity, parseInt(input.value) + delta));
  input.value = val;
};

window.addToCartFromDetail = function () {
  const p = State.currentProduct;
  const qty = parseInt(document.getElementById('detail-qty').value);
  State.addToCart(p.id, p.name, p.price, qty);
  Toast.success(`${p.name} added to cart`);
  App.updateNavAuth();
};

// Auth
function LoginView() {
  render(`
    <div class="auth-page">
      <h1>Welcome back</h1>
      <p class="subtitle">Sign in to your SmartShop account</p>
      <div class="card">
        <div id="login-alert"></div>
        <form id="login-form" onsubmit="App.login(event)">
          <div class="form-group">
            <label class="form-label" for="login-email">Email</label>
            <input class="form-input" id="login-email" type="email" required placeholder="you@example.com" autocomplete="email">
          </div>
          <div class="form-group">
            <label class="form-label" for="login-password">Password</label>
            <input class="form-input" id="login-password" type="password" required placeholder="Your password" autocomplete="current-password">
          </div>
          <button type="submit" class="btn btn--primary btn--full" id="login-submit">Sign In</button>
        </form>
        <div class="auth-page__footer">Don't have an account? <a href="#/register">Create one</a></div>
      </div>
    </div>
  `);
}

function RegisterView() {
  render(`
    <div class="auth-page">
      <h1>Create account</h1>
      <p class="subtitle">Join SmartShop today</p>
      <div class="card">
        <div id="register-alert"></div>
        <form id="register-form" onsubmit="App.register(event)">
          <div class="form-group">
            <label class="form-label" for="reg-name">Full Name</label>
            <input class="form-input" id="reg-name" type="text" required placeholder="John Doe" minlength="2" maxlength="100" autocomplete="name">
          </div>
          <div class="form-group">
            <label class="form-label" for="reg-email">Email</label>
            <input class="form-input" id="reg-email" type="email" required placeholder="you@example.com" autocomplete="email">
          </div>
          <div class="form-group">
            <label class="form-label" for="reg-password">Password</label>
            <input class="form-input" id="reg-password" type="password" required placeholder="Min 8 characters" minlength="8" autocomplete="new-password">
          </div>
          <button type="submit" class="btn btn--primary btn--full" id="reg-submit">Create Account</button>
        </form>
        <div class="auth-page__footer">Already have an account? <a href="#/login">Sign in</a></div>
      </div>
    </div>
  `);
}

// Orders
async function OrdersView() {
  if (!State.isLoggedIn) { Router.navigate('/login'); return; }

  render(`
    <div class="page-header"><h1>My Orders</h1></div>
    <div class="table-wrapper"><table id="orders-table">
      <thead><tr><th>Order #</th><th>Date</th><th>Items</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody id="orders-tbody"></tbody>
    </table></div>
    <div class="pagination" id="orders-pagination"></div>
  `);

  loadOrders(0);
}

async function loadOrders(page = 0) {
  const tbody = document.getElementById('orders-tbody');
  if (!tbody) return;

  try {
    const data = await http.get('/orders', { userId: State.user.id, page, size: 10 });
    State.orders = data.content || [];

    if (State.orders.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><h3>No orders yet</h3><p>Browse products to place your first order</p></div></td></tr>';
      return;
    }

    tbody.innerHTML = State.orders.map(o => `
      <tr>
        <td style="font-weight:600">${o.orderNumber}</td>
        <td>${formatDate(o.createdAt)}</td>
        <td>${o.items?.length || 0}</td>
        <td style="font-weight:600">${formatPrice(o.totalAmount)}</td>
        <td>${statusBadge(o.status)}</td>
        <td class="table-actions">
          <button class="btn btn--secondary btn--sm" onclick="viewOrder('${o.orderNumber}')">View</button>
          ${o.status === 'PENDING' ? `<button class="btn btn--danger btn--sm" onclick="cancelOrder(${o.id})">Cancel</button>` : ''}
        </td>
      </tr>
    `).join('');

    renderPagination('orders-pagination', data, loadOrders);
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="alert alert--error">${e.message}</div></td></tr>`;
  }
}

window.viewOrder = async function (orderNumber) {
  try {
    const order = await http.get('/orders', { orderNumber });
    Modal.open(`
      <h2>Order ${order.orderNumber}</h2>
      <div style="margin-bottom:16px">${statusBadge(order.status)}</div>
      <table style="width:100%;margin:16px 0;font-size:0.85rem">
        <thead><tr><th>Product</th><th>SKU</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
        <tbody>
          ${order.items.map(i => `<tr><td>${i.productName}</td><td>${i.productSku || '—'}</td><td>${i.quantity}</td><td>${formatPrice(i.price)}</td><td>${formatPrice(i.lineTotal)}</td></tr>`).join('')}
        </tbody>
      </table>
      <div style="display:flex;justify-content:space-between;padding-top:12px;border-top:1px solid var(--border)">
        <span style="font-weight:600">Total</span><span style="font-weight:700;color:var(--accent)">${formatPrice(order.totalAmount)}</span>
      </div>
      ${order.shippingAddress ? `<p style="margin-top:12px;color:var(--text-2);font-size:0.85rem">Shipping: ${order.shippingAddress}</p>` : ''}
    `);
  } catch (e) { Toast.error(e.message); }
};

window.cancelOrder = async function (id) {
  if (!confirm('Cancel this order?')) return;
  try {
    await http.patch(`/orders/${id}/cancel`);
    Toast.success('Order cancelled');
    loadOrders(State.currentPage);
  } catch (e) { Toast.error(e.message); }
};

// Payments
async function PaymentsView() {
  if (!State.isLoggedIn) { Router.navigate('/login'); return; }

  render(`
    <div class="page-header"><h1>Payments</h1></div>
    <div class="table-wrapper"><table id="payments-table">
      <thead><tr><th>Transaction</th><th>Order</th><th>Amount</th><th>Method</th><th>Status</th><th>Date</th><th>Actions</th></tr></thead>
      <tbody id="payments-tbody"></tbody>
    </table></div>
    <div class="pagination" id="payments-pagination"></div>
  `);

  loadPayments(0);
}

async function loadPayments(page = 0) {
  const tbody = document.getElementById('payments-tbody');
  if (!tbody) return;

  try {
    const data = await http.get('/payments', { userId: State.user.id, page, size: 10 });
    State.payments = data.content || data;

    if (State.payments.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7"><div class="empty-state"><h3>No payments yet</h3></div></td></tr>';
      return;
    }

    tbody.innerHTML = State.payments.map(p => `
      <tr>
        <td style="font-family:monospace;font-size:0.8rem">${p.transactionId}</td>
        <td>${p.orderNumber}</td>
        <td style="font-weight:600">${formatPrice(p.amount)}</td>
        <td>${p.paymentMethod}</td>
        <td>${statusBadge(p.status)}</td>
        <td>${formatDate(p.createdAt)}</td>
        <td class="table-actions">
          ${p.status === 'COMPLETED' && State.isAdmin ? `<button class="btn btn--danger btn--sm" onclick="refundPayment(${p.id})">Refund</button>` : ''}
        </td>
      </tr>
    `).join('');

    renderPagination('payments-pagination', { page: data.page || 0, totalPages: data.totalPages || 1 }, loadPayments);
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="alert alert--error">${e.message}</div></td></tr>`;
  }
}

window.refundPayment = async function (id) {
  if (!confirm('Refund this payment?')) return;
  try {
    await http.patch(`/payments/${id}/refund`);
    Toast.success('Payment refunded');
    loadPayments(State.currentPage);
  } catch (e) { Toast.error(e.message); }
};

// Checkout
function CheckoutView() {
  if (!State.isLoggedIn) { Router.navigate('/login'); return; }
  if (State.cart.length === 0) { Router.navigate('/products'); return; }

  render(`
    <div style="margin-bottom:20px">
      <a href="#/products" style="color:var(--text-2);text-decoration:none;font-size:0.9rem">← Continue shopping</a>
    </div>
    <h1>Checkout</h1>
    <div style="display:grid;grid-template-columns:1fr 380px;gap:32px;margin-top:24px;align-items:start">
      <div>
        <h2 style="margin-bottom:16px">Shipping</h2>
        <div class="card">
          <div class="form-group">
            <label class="form-label" for="checkout-address">Shipping Address</label>
            <textarea class="form-input" id="checkout-address" placeholder="123 Main St, City, State 12345"></textarea>
          </div>
        </div>
        <h2 style="margin:24px 0 16px">Payment Method</h2>
        <div class="card">
          <div class="form-group">
            <label class="form-label" for="checkout-method">Method</label>
            <select class="form-input" id="checkout-method">
              <option value="CREDIT_CARD">Credit Card</option>
              <option value="DEBIT_CARD">Debit Card</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="DIGITAL_WALLET">Digital Wallet</option>
            </select>
          </div>
        </div>
      </div>
      <div class="card" style="position:sticky;top:32px">
        <h3 style="margin-bottom:16px">Order Summary</h3>
        ${State.cart.map(i => `
          <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border);font-size:0.9rem">
            <span>${i.name} × ${i.quantity}</span>
            <span style="font-weight:600">${formatPrice(i.price * i.quantity)}</span>
          </div>
        `).join('')}
        <div style="display:flex;justify-content:space-between;padding:16px 0 0;font-weight:700;font-size:1.1rem">
          <span>Total</span><span style="color:var(--accent)">${formatPrice(State.cartTotal)}</span>
        </div>
        <button class="btn btn--primary btn--full" style="margin-top:16px" onclick="App.placeOrder()" id="checkout-submit">Place Order</button>
        <div id="checkout-alert" style="margin-top:12px"></div>
      </div>
    </div>
  `);
}

window.placeOrder = async function () {
  const btn = document.getElementById('checkout-submit');
  const alertEl = document.getElementById('checkout-alert');
  btn.disabled = true;
  btn.textContent = 'Processing...';
  alertEl.innerHTML = '';

  try {
    // Step 1: Create order
    const orderBody = {
      userId: State.user.id,
      shippingAddress: document.getElementById('checkout-address').value,
      items: State.cart.map(i => ({ productId: i.productId, quantity: i.quantity })),
    };

    const order = await http.post('/orders', orderBody);

    // Step 2: Process payment
    const paymentBody = {
      orderNumber: order.orderNumber,
      userId: State.user.id,
      amount: order.totalAmount,
      paymentMethod: document.getElementById('checkout-method').value,
      userEmail: State.user.email,
    };

    await http.post('/payments', paymentBody);

    State.clearCart();
    Toast.success('Order placed successfully!');
    Router.navigate('/orders');
  } catch (e) {
    alertEl.innerHTML = `<div class="alert alert--error">${e.message}</div>`;
    btn.disabled = false;
    btn.textContent = 'Place Order';
  }
};

// Admin
async function AdminView() {
  if (!State.isAdmin) { Router.navigate('/'); return; }

  render(`
    <div class="page-header">
      <h1>Admin Dashboard</h1>
    </div>
    <div class="stats-row" id="admin-stats"></div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>Products</h2>
      <button class="btn btn--primary" onclick="showCreateProduct()">+ New Product</button>
    </div>
    <div class="table-wrapper"><table id="admin-products-table">
      <thead><tr><th>SKU</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody id="admin-products-tbody"></tbody>
    </table></div>
    <div class="pagination" id="admin-pagination"></div>

    <h2 style="margin:32px 0 16px">All Orders</h2>
    <div class="table-wrapper"><table>
      <thead><tr><th>Order #</th><th>User</th><th>Date</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody id="admin-orders-tbody"></tbody>
    </table></div>
  `);

  loadAdminProducts(0);
  loadAdminStats();
}

async function loadAdminStats() {
  try {
    const [ordersData, productsData] = await Promise.all([
      http.get('/orders', { page: 0, size: 1 }),
      http.get('/products', { page: 0, size: 1 }),
    ]);
    document.getElementById('admin-stats').innerHTML = `
      <div class="stat-card"><div class="stat-card__label">Products</div><div class="stat-card__value">${productsData.totalElements || 0}</div></div>
      <div class="stat-card"><div class="stat-card__label">Orders</div><div class="stat-card__value">${ordersData.totalElements || 0}</div></div>
    `;
  } catch (e) {}
}

async function loadAdminProducts(page = 0) {
  const tbody = document.getElementById('admin-products-tbody');
  if (!tbody) return;

  try {
    const data = await http.get('/products', { page, size: 10 });
    if (!data.content || data.content.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7"><div class="empty-state"><h3>No products</h3></div></td></tr>';
      return;
    }

    tbody.innerHTML = data.content.map(p => `
      <tr>
        <td style="font-family:monospace">${p.sku}</td>
        <td style="font-weight:600">${p.name}</td>
        <td><span class="product-card__category">${p.category}</span></td>
        <td>${formatPrice(p.price)}</td>
        <td class="${p.quantity <= 5 ? 'product-card__stock--low' : ''}">${p.quantity}</td>
        <td>${p.active ? statusBadge('ACTIVE') : statusBadge('INACTIVE')}</td>
        <td class="table-actions">
          <button class="btn btn--secondary btn--sm" onclick="showEditProduct(${p.id})">Edit</button>
          <button class="btn btn--danger btn--sm" onclick="deleteProduct(${p.id})">Delete</button>
        </td>
      </tr>
    `).join('');

    renderPagination('admin-pagination', data, loadAdminProducts);
    loadAdminOrders();
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="alert alert--error">${e.message}</div></td></tr>`;
  }
}

async function loadAdminOrders() {
  const tbody = document.getElementById('admin-orders-tbody');
  if (!tbody) return;

  try {
    const data = await http.get('/orders', { page: 0, size: 20 });
    const orders = data.content || [];

    if (orders.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><h3>No orders</h3></div></td></tr>';
      return;
    }

    tbody.innerHTML = orders.map(o => `
      <tr>
        <td style="font-weight:600">${o.orderNumber}</td>
        <td>${o.userEmail || o.userId}</td>
        <td>${formatDate(o.createdAt)}</td>
        <td style="font-weight:600">${formatPrice(o.totalAmount)}</td>
        <td>${statusBadge(o.status)}</td>
        <td class="table-actions">
          <select class="form-input" style="width:140px;padding:4px 8px;font-size:0.8rem" onchange="updateOrderStatus(${o.id}, this.value)">
            <option value="" disabled selected>Change status</option>
            <option value="CONFIRMED">Confirm</option>
            <option value="SHIPPED">Ship</option>
            <option value="DELIVERED">Deliver</option>
          </select>
        </td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="alert alert--error">${e.message}</div></td></tr>`;
  }
}

window.updateOrderStatus = async function (id, status) {
  try {
    await http.patch(`/orders/${id}/status`, null, { status });
    Toast.success(`Order updated to ${status}`);
    loadAdminOrders();
  } catch (e) { Toast.error(e.message); }
};

window.showCreateProduct = function () {
  Modal.open(`
    <h2>New Product</h2>
    <form onsubmit="createProduct(event)">
      <div class="form-group"><label class="form-label">Name</label><input class="form-input" name="name" required minlength="2" maxlength="200"></div>
      <div class="form-group"><label class="form-label">SKU</label><input class="form-input" name="sku" required maxlength="50"></div>
      <div class="form-group"><label class="form-label">Category</label>
        <select class="form-input" name="category" required>
          <option value="ELECTRONICS">Electronics</option><option value="CLOTHING">Clothing</option>
          <option value="BOOKS">Books</option><option value="HOME">Home</option>
          <option value="SPORTS">Sports</option><option value="TOYS">Toys</option>
          <option value="FOOD">Food</option><option value="OTHER">Other</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">Price</label><input class="form-input" type="number" name="price" step="0.01" min="0.01" required></div>
      <div class="form-group"><label class="form-label">Quantity</label><input class="form-input" type="number" name="quantity" min="1" required></div>
      <div class="form-group"><label class="form-label">Description</label><textarea class="form-input" name="description" maxlength="1000"></textarea></div>
      <div class="form-group"><label class="form-label">Image URL</label><input class="form-input" name="imageUrl"></div>
      <button type="submit" class="btn btn--primary btn--full">Create Product</button>
    </form>
  `);
};

window.createProduct = async function (e) {
  e.preventDefault();
  const form = e.target;
  const data = Object.fromEntries(new FormData(form));
  data.price = parseFloat(data.price);
  data.quantity = parseInt(data.quantity);

  try {
    await http.post('/products', data);
    Toast.success('Product created');
    Modal.close();
    loadAdminProducts(0);
  } catch (err) { Toast.error(err.message); }
};

window.showEditProduct = async function (id) {
  try {
    const p = await http.get(`/products/${id}`);
    Modal.open(`
      <h2>Edit Product</h2>
      <form onsubmit="updateProduct(event, ${id})">
        <div class="form-group"><label class="form-label">Name</label><input class="form-input" name="name" value="${p.name}" required></div>
        <div class="form-group"><label class="form-label">SKU</label><input class="form-input" name="sku" value="${p.sku}" required></div>
        <div class="form-group"><label class="form-label">Category</label>
          <select class="form-input" name="category" required>
            ${['ELECTRONICS','CLOTHING','BOOKS','HOME','SPORTS','TOYS','FOOD','OTHER'].map(c => `<option value="${c}" ${c === p.category ? 'selected' : ''}>${c}</option>`).join('')}
          </select>
        </div>
        <div class="form-group"><label class="form-label">Price</label><input class="form-input" type="number" name="price" step="0.01" value="${p.price}" required></div>
        <div class="form-group"><label class="form-label">Quantity</label><input class="form-input" type="number" name="quantity" value="${p.quantity}" min="0" required></div>
        <div class="form-group"><label class="form-label">Description</label><textarea class="form-input" name="description">${p.description || ''}</textarea></div>
        <button type="submit" class="btn btn--primary btn--full">Save Changes</button>
      </form>
    `);
  } catch (err) { Toast.error(err.message); }
};

window.updateProduct = async function (e, id) {
  e.preventDefault();
  const form = e.target;
  const data = Object.fromEntries(new FormData(form));
  data.price = parseFloat(data.price);
  data.quantity = parseInt(data.quantity);

  try {
    await http.put(`/products/${id}`, data);
    Toast.success('Product updated');
    Modal.close();
    loadAdminProducts(State.currentPage);
  } catch (err) { Toast.error(err.message); }
};

window.deleteProduct = async function (id) {
  if (!confirm('Delete this product?')) return;
  try {
    await http.del(`/products/${id}`);
    Toast.success('Product deleted');
    loadAdminProducts(0);
  } catch (e) { Toast.error(e.message); }
};

// ============================================================
// Toast
// ============================================================
const Toast = {
  show(msg, type = 'info', duration = 3000) {
    const container = document.getElementById('toast-container') || (() => {
      const el = document.createElement('div');
      el.id = 'toast-container';
      el.className = 'toast-container';
      document.body.appendChild(el);
      return el;
    })();
    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.remove(), 300); }, duration);
  },
  success(msg) { this.show(msg, 'success'); },
  error(msg) { this.show(msg, 'error'); },
  info(msg) { this.show(msg, 'info'); },
};

// ============================================================
// Modal
// ============================================================
const Modal = {
  open(html) {
    let overlay = document.getElementById('modal-overlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'modal-overlay';
      overlay.className = 'modal-overlay';
      overlay.onclick = (e) => { if (e.target === overlay) this.close(); };
      document.body.appendChild(overlay);
    }
    overlay.innerHTML = `<div class="modal">${html}</div>`;
    requestAnimationFrame(() => overlay.classList.add('open'));
  },
  close() {
    const overlay = document.getElementById('modal-overlay');
    if (overlay) {
      overlay.classList.remove('open');
      setTimeout(() => overlay.remove(), 200);
    }
  }
};

// ============================================================
// App
// ============================================================
const App = {
  init() {
    Router.add('/', HomeView);
    Router.add('/products', ProductsView);
    Router.add('/products/:id', ProductDetailView);
    Router.add('/login', LoginView);
    Router.add('/register', RegisterView);
    Router.add('/checkout', CheckoutView);
    Router.add('/orders', OrdersView);
    Router.add('/payments', PaymentsView);
    Router.add('/admin', AdminView);

    this.updateNavAuth();
    Router.init();
  },

  updateNavAuth() {
    const authBtns = document.getElementById('auth-buttons');
    const userSection = document.getElementById('user-section');
    const logoutBtn = document.getElementById('logout-btn');
    const authOnly = document.querySelectorAll('.auth-only');
    const adminOnly = document.querySelectorAll('.admin-only');

    if (State.isLoggedIn) {
      authBtns.style.display = 'none';
      userSection.style.display = 'flex';
      logoutBtn.style.display = 'flex';
      document.getElementById('user-name').textContent = State.user.name;
      document.getElementById('user-role').textContent = State.user.role;
      document.getElementById('user-avatar').textContent = State.user.name?.charAt(0).toUpperCase() || '?';
      authOnly.forEach(el => el.style.display = '');
    } else {
      authBtns.style.display = 'flex';
      userSection.style.display = 'none';
      logoutBtn.style.display = 'none';
      authOnly.forEach(el => el.style.display = 'none');
    }

    if (State.isAdmin) { adminOnly.forEach(el => el.style.display = ''); }
    else { adminOnly.forEach(el => el.style.display = 'none'); }
  },

  async login(e) {
    e.preventDefault();
    const alertEl = document.getElementById('login-alert');
    const btn = document.getElementById('login-submit');
    btn.disabled = true;
    alertEl.innerHTML = '';

    try {
      const data = await http.post('/auth/login', {
        email: document.getElementById('login-email').value,
        password: document.getElementById('login-password').value,
      });
      State.setAuth(data.token, { id: null, name: data.name, email: data.email, role: data.role });

      // Fetch user ID
      const userResp = await http.get('/users', { email: data.email });
      if (userResp?.id) { State.user.id = userResp.id; localStorage.setItem('user', JSON.stringify(State.user)); }

      this.updateNavAuth();
      Toast.success(`Welcome back, ${data.name}!`);
      Router.navigate('/');
    } catch (err) {
      alertEl.innerHTML = `<div class="alert alert--error">${err.message}</div>`;
    } finally {
      btn.disabled = false;
    }
  },

  async register(e) {
    e.preventDefault();
    const alertEl = document.getElementById('register-alert');
    const btn = document.getElementById('reg-submit');
    btn.disabled = true;
    alertEl.innerHTML = '';

    try {
      await http.post('/auth/register', {
        name: document.getElementById('reg-name').value,
        email: document.getElementById('reg-email').value,
        password: document.getElementById('reg-password').value,
      });
      Toast.success('Account created! Please sign in.');
      Router.navigate('/login');
    } catch (err) {
      alertEl.innerHTML = `<div class="alert alert--error">${err.message}</div>`;
    } finally {
      btn.disabled = false;
    }
  },

  logout() {
    State.clearAuth();
    this.updateNavAuth();
    Toast.info('Signed out');
    Router.navigate('/');
  },
};

// Boot
document.addEventListener('DOMContentLoaded', () => App.init());
