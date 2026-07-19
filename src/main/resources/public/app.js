const api = (url, opts = {}) =>
    fetch(url, { headers: { 'Content-Type': 'application/json' }, ...opts }).then(r => r.json());

let sesion = null;

function render(html) { document.getElementById('main').innerHTML = html; }
function nav(html) { document.getElementById('nav').innerHTML = html; }

async function init() {
    const s = await api('/api/sesion');
    if (s.activo) { sesion = s; }
    mostrarVista();
}

function mostrarVista() {
    if (!sesion) return mostrarLogin();
    if (sesion.tipo === 'ADMINISTRADOR') return mostrarProductosAdmin();
    return mostrarProductosCliente();
}

// ═══════════════ LOGIN ═══════════════
function mostrarLogin() {
    nav('');
    render(`
        <div class="login-box">
            <h2>eGESVEN</h2>
            <input id="user" placeholder="Username"><br>
            <input id="pass" type="password" placeholder="Password"><br>
            <button onclick="login()">Entrar</button>
            <p><a href="#" onclick="mostrarRegistro()">Crear cuenta</a></p>
            <p id="msg"></p>
        </div>
    `);
}

async function login() {
    const username = document.getElementById('user').value;
    const password = document.getElementById('pass').value;
    const r = await api('/api/login', { method: 'POST', body: JSON.stringify({ username, password }) });
    if (!r.ok) return document.getElementById('msg').innerText = r.error;
    render(`
        <div class="login-box">
            <h2>Verificacion MFA</h2>
            <p style="background:#e8f5e9;padding:12px;border-radius:4px;font-size:1.3em;text-align:center">
                Codigo: <strong>${r.codigo}</strong>
            </p>
            <input id="mfa" placeholder="Ingresa el codigo de arriba"><br>
            <button onclick="verificarMFA()">Verificar</button>
            <p id="msg"></p>
        </div>
    `);
}

async function verificarMFA() {
    const codigo = document.getElementById('mfa').value;
    const r = await api('/api/mfa', { method: 'POST', body: JSON.stringify({ codigo }) });
    if (!r.ok) return document.getElementById('msg').innerText = r.error;
    sesion = { tipo: r.tipo, nombre: r.nombre };
    mostrarVista();
}

// ═══════════════ REGISTRO ═══════════════
function mostrarRegistro() {
    nav('');
    render(`
        <div class="login-box">
            <h2>Crear cuenta</h2>
            <input id="reg-nombre" placeholder="Nombre completo"><br>
            <input id="reg-email" placeholder="Email"><br>
            <input id="reg-user" placeholder="Username"><br>
            <input id="reg-pass" type="password" placeholder="Password"><br>
            <input id="reg-dir" placeholder="Direccion de envio"><br>
            <button onclick="registrar()">Registrarse</button>
            <p><a href="#" onclick="mostrarLogin()">Ya tengo cuenta</a></p>
            <p id="msg"></p>
        </div>
    `);
}

async function registrar() {
    const body = {
        nombre: document.getElementById('reg-nombre').value,
        email: document.getElementById('reg-email').value,
        username: document.getElementById('reg-user').value,
        password: document.getElementById('reg-pass').value,
        direccion: document.getElementById('reg-dir').value
    };
    const r = await api('/api/registro', { method: 'POST', body: JSON.stringify(body) });
    if (!r.ok) return document.getElementById('msg').innerText = r.error;
    alert('Registro exitoso. Ya puedes iniciar sesion.');
    mostrarLogin();
}

async function logout() {
    await api('/api/logout', { method: 'POST' });
    sesion = null;
    mostrarVista();
}

// ═══════════════ CLIENTE ═══════════════
async function mostrarProductosCliente() {
    nav(`
        <button onclick="mostrarProductosCliente()">Productos</button>
        <button onclick="mostrarCarrito()">Carrito</button>
        <button onclick="mostrarPedidos()">Mis pedidos</button>
        <button class="out" onclick="logout()">Salir</button>
    `);
    await cargarCatalogo();
}

// RF005: filtros activos del catalogo
let filtros = { q: '', categoria: '' };

async function cargarCatalogo() {
    const params = new URLSearchParams();
    if (filtros.q) params.set('q', filtros.q);
    if (filtros.categoria) params.set('categoria', filtros.categoria);
    const query = params.toString();

    const [productos, categorias] = await Promise.all([
        api('/api/productos' + (query ? '?' + query : '')),
        api('/api/categorias')
    ]);

    let html = `<h2>Catalogo — ${sesion.nombre}</h2>
        <div class="buscador">
            <input id="bus-q" placeholder="Buscar por nombre o descripcion"
                   value="${filtros.q}" onkeydown="if(event.key==='Enter')buscar()">
            <select id="bus-cat">
                <option value="">Todas las categorias</option>
                ${categorias.map(c =>
                    `<option value="${c}" ${filtros.categoria === c ? 'selected' : ''}>${c}</option>`
                ).join('')}
            </select>
            <button onclick="buscar()">Buscar</button>
            ${(filtros.q || filtros.categoria)
                ? '<button class="out" onclick="limpiarBusqueda()">Limpiar</button>' : ''}
        </div>`;

    if (!productos.length) {
        html += '<p>No se encontraron productos con esos criterios.</p>';
    }
    html += '<div class="grid">';
    productos.forEach(p => {
        html += `<div class="card">
            ${p.imagenUrl ? `<img class="prod-img clickable" src="${p.imagenUrl}" alt="${p.nombre}" onclick="mostrarDetalleProducto(${p.idProducto})">` : ''}
            <h3 class="clickable" onclick="mostrarDetalleProducto(${p.idProducto})">${p.nombre}</h3>
            <p>${p.descripcion || ''}</p>
            <p class="price">$${p.precio.toLocaleString()}</p>
            <small>Stock: ${p.stock} | ${p.categoria || 'Sin categoria'}</small>
            <br>
            <input id="qty-${p.idProducto}" type="number" value="1" min="1" max="${p.stock}" style="width:50px">
            <button onclick="agregarAlCarrito(${p.idProducto})">Agregar</button>
            <button onclick="mostrarDetalleProducto(${p.idProducto})">Ver detalle</button>
        </div>`;
    });
    html += '</div>';
    render(html);
}

function buscar() {
    filtros = {
        q: document.getElementById('bus-q').value.trim(),
        categoria: document.getElementById('bus-cat').value
    };
    cargarCatalogo();
}

function limpiarBusqueda() {
    filtros = { q: '', categoria: '' };
    cargarCatalogo();
}

async function mostrarDetalleProducto(idProducto) {
    const productos = await api('/api/productos');
    const p = productos.find(x => x.idProducto === idProducto);
    if (!p) return;
    render(`
        <button class="out" onclick="mostrarProductosCliente()">← Volver al catalogo</button>
        <div class="detalle">
            ${p.imagenUrl
                ? `<img class="detalle-img" src="${p.imagenUrl}" alt="${p.nombre}">`
                : `<div class="detalle-img sin-img">Sin imagen</div>`}
            <div class="detalle-info">
                <h2>${p.nombre}</h2>
                <p class="price">$${p.precio.toLocaleString()}</p>
                <p>${p.descripcion || 'Sin descripcion.'}</p>
                <p><strong>Categoria:</strong> ${p.categoria || 'Sin categoria'}</p>
                <p><strong>Stock disponible:</strong> ${p.stock}</p>
                <hr>
                <label>Cantidad:
                    <input id="qty-${p.idProducto}" type="number" value="1" min="1" max="${p.stock}" style="width:60px">
                </label>
                <button onclick="agregarAlCarrito(${p.idProducto})">Agregar al carrito</button>
            </div>
        </div>
    `);
}

async function agregarAlCarrito(idProducto) {
    const qty = document.getElementById('qty-' + idProducto);
    const cantidad = qty ? parseInt(qty.value) : 1;
    if (cantidad < 1) return;
    await api('/api/carrito', { method: 'POST', body: JSON.stringify({ idProducto, cantidad }) });
    alert('Agregado al carrito');
}

async function mostrarCarrito() {
    const c = await api('/api/carrito');
    let html = '<h2>Tu Carrito</h2>';
    if (!c.items || c.items.length === 0) {
        html += '<p>Carrito vacio.</p>';
    } else {
        c.items.forEach(i => {
            html += `<div class="card">
                <p><strong>${i.cantidad}x</strong> ${i.producto.nombre} — $${i.producto.precio.toLocaleString()} c/u — Subtotal: $${(i.producto.precio * i.cantidad).toLocaleString()}
                <button class="out" onclick="quitarDelCarrito(${i.idItem})">Quitar</button>
                </p>
            </div>`;
        });
        html += `<div class="card">
            <p>Subtotal: $${c.subtotal.toLocaleString()}</p>
            <p>IVA (19%): $${c.impuesto.toLocaleString()}</p>
            <p>Envio: $${c.envio.toLocaleString()}</p>
            <p><strong>Total: $${c.total.toLocaleString()}</strong></p>
            <button onclick="comprar()">Comprar</button>
        </div>`;
    }
    render(html);
}

async function quitarDelCarrito(idItem) {
    await api('/api/carrito/' + idItem, { method: 'DELETE' });
    mostrarCarrito();
}

async function comprar() {
    const medioPago = confirm('Pagar con Transbank? (Aceptar = Transbank, Cancelar = PayPal)')
        ? 'TRANSBANK' : 'PAYPAL';
    if (!confirm(`Confirmar compra con ${medioPago}?`)) return;
    const r = await api('/api/comprar', { method: 'POST', body: JSON.stringify({ medioPago }) });
    if (r.ok) {
        alert(`Compra exitosa! Pedido #${r.idPedido} — Total: $${r.total}`);
        mostrarVista();
    } else {
        alert('Error: ' + r.error);
    }
}

async function mostrarPedidos() {
    const pedidos = await api('/api/pedidos');
    let html = '<h2>Mis Pedidos</h2>';
    if (!pedidos.length) { html += '<p>No tienes pedidos.</p>'; }
    else {
        pedidos.forEach(p => {
            html += `<div class="card">
                <p><strong>Pedido #${p.idPedido}</strong></p>
                <p>Estado: <span class="estado">${p.estado}</span> | Total: <strong>$${p.total ? p.total.toLocaleString() : '0'}</strong></p>
                <small>${p.direccionEnvio || ''}</small>
                <br>
                <button onclick="verDetallePedido(${p.idPedido})">Ver detalle</button>
                <button onclick="mostrarRecibo(${p.idPedido})">Ver recibo</button>
                <div id="detalle-${p.idPedido}"></div>
            </div>`;
        });
    }
    render(html);
}

async function verDetallePedido(idPedido) {
    const el = document.getElementById('detalle-' + idPedido);
    if (el.innerHTML) { el.innerHTML = ''; return; }
    const p = await api('/api/pedidos/' + idPedido);
    let html = '<hr><strong>Items:</strong><ul>';
    if (p.detalles) {
        p.detalles.forEach(d => {
            const nombre = d.nombreProducto || ('Producto #' + d.idProducto);
            html += `<li>${d.cantidad}x ${nombre} — $${d.precioUnitario.toLocaleString()} c/u — Subtotal: $${d.subtotal.toLocaleString()}</li>`;
        });
    }
    html += `</ul>
        <p>Subtotal: $${p.subtotal.toLocaleString()} | IVA: $${p.impuesto.toLocaleString()} | Envio: $${p.costoEnvio.toLocaleString()}</p>
        <p><strong>Total: $${p.total.toLocaleString()}</strong></p>`;
    el.innerHTML = html;
}

// ═══════════════ RECIBO (RF010) ═══════════════
async function mostrarRecibo(idPedido) {
    const r = await api('/api/pedidos/' + idPedido + '/recibo');
    if (r.error) return alert('Error: ' + r.error);

    const volver = sesion.tipo === 'ADMINISTRADOR' ? 'mostrarAdminPedidos()' : 'mostrarPedidos()';
    let items = '';
    (r.detalles || []).forEach(d => {
        const nombre = d.nombreProducto || ('Producto #' + d.idProducto);
        items += `<tr>
            <td>${d.cantidad}</td>
            <td>${nombre}</td>
            <td class="num">$${d.precioUnitario.toLocaleString()}</td>
            <td class="num">$${d.subtotal.toLocaleString()}</td>
        </tr>`;
    });

    render(`
        <button class="out" onclick="${volver}">← Volver</button>
        <div class="recibo">
            <h2>RECIBO ${r.numero}</h2>
            <p class="recibo-meta">
                Emitido: ${r.fechaEmision}<br>
                Cliente: <strong>${r.nombreCliente}</strong> (${r.emailCliente})<br>
                Envio a: ${r.direccionEnvio}<br>
                Estado del pedido: <span class="estado">${r.estadoPedido}</span>
            </p>
            <table class="recibo-tabla">
                <thead>
                    <tr><th>Cant.</th><th>Producto</th><th class="num">P. unitario</th><th class="num">Subtotal</th></tr>
                </thead>
                <tbody>${items}</tbody>
            </table>
            <table class="recibo-totales">
                <tr><td>Subtotal</td><td class="num">$${r.subtotal.toLocaleString()}</td></tr>
                <tr><td>IVA (19%)</td><td class="num">$${r.impuesto.toLocaleString()}</td></tr>
                <tr><td>Envio</td><td class="num">$${r.costoEnvio.toLocaleString()}</td></tr>
                <tr class="total"><td><strong>TOTAL</strong></td><td class="num"><strong>$${r.total.toLocaleString()}</strong></td></tr>
            </table>
            <p class="recibo-pago">
                Pago: <strong>${r.medioPago}</strong> (${r.estadoPago})<br>
                Token: <code>${r.tokenTransaccion}</code>
            </p>
            <button onclick="window.print()">Imprimir</button>
        </div>
    `);
}

// ═══════════════ ADMIN ═══════════════
async function mostrarProductosAdmin() {
    nav(`
        <button onclick="mostrarProductosAdmin()">Productos</button>
        <button onclick="mostrarAdminPedidos()">Pedidos</button>
        <button onclick="mostrarLogs()">Logs</button>
        <button class="out" onclick="logout()">Salir</button>
    `);
    const productos = await api('/api/productos');
    let html = `<h2>Gestion de Productos — ${sesion.nombre}</h2>
        <button onclick="mostrarFormProducto()">+ Nuevo producto</button>
        <hr><div class="grid">`;
    productos.forEach(p => {
        html += `<div class="card">
            ${p.imagenUrl ? `<img class="prod-img" src="${p.imagenUrl}" alt="${p.nombre}">` : ''}
            <h3>${p.nombre} <small>[ID: ${p.idProducto}]</small></h3>
            <p>${p.descripcion || ''}</p>
            <p class="price">$${p.precio.toLocaleString()}</p>
            <small>Stock: ${p.stock} | ${p.categoria || 'Sin categoria'}</small>
            <br>
            <button onclick="mostrarFormProducto(${p.idProducto})">Editar</button>
            <button class="out" onclick="eliminarProducto(${p.idProducto},'${p.nombre}')">Eliminar</button>
        </div>`;
    });
    html += '</div>';
    render(html);
}

function mostrarFormProducto(id) {
    if (id) {
        api('/api/productos').then(productos => {
            const p = productos.find(x => x.idProducto === id);
            if (!p) return;
            render(`
                <div class="login-box" style="max-width:500px">
                    <h2>Editar producto</h2>
                    <input id="pf-nombre" value="${p.nombre}"><br>
                    <input id="pf-desc" value="${p.descripcion || ''}"><br>
                    <input id="pf-precio" type="number" value="${p.precio}"><br>
                    <input id="pf-stock" type="number" value="${p.stock}"><br>
                    <input id="pf-cat" value="${p.categoria || ''}"><br>
                    <input id="pf-img" value="${p.imagenUrl || ''}" placeholder="URL de la imagen"><br>
                    <button onclick="guardarProducto(${id})">Guardar</button>
                    <button class="out" onclick="mostrarProductosAdmin()">Cancelar</button>
                </div>
            `);
        });
    } else {
        render(`
            <div class="login-box" style="max-width:500px">
                <h2>Nuevo producto</h2>
                <input id="pf-nombre" placeholder="Nombre"><br>
                <input id="pf-desc" placeholder="Descripcion"><br>
                <input id="pf-precio" type="number" placeholder="Precio"><br>
                <input id="pf-stock" type="number" placeholder="Stock"><br>
                <input id="pf-cat" placeholder="Categoria"><br>
                <input id="pf-img" placeholder="URL de la imagen"><br>
                <button onclick="guardarProducto()">Crear</button>
                <button class="out" onclick="mostrarProductosAdmin()">Cancelar</button>
            </div>
        `);
    }
}

async function guardarProducto(id) {
    const body = {
        nombre: document.getElementById('pf-nombre').value,
        descripcion: document.getElementById('pf-desc').value,
        precio: parseInt(document.getElementById('pf-precio').value),
        stock: parseInt(document.getElementById('pf-stock').value),
        categoria: document.getElementById('pf-cat').value,
        imagenUrl: document.getElementById('pf-img').value
    };
    if (id) {
        await api('/api/productos/' + id, { method: 'PUT', body: JSON.stringify(body) });
    } else {
        await api('/api/productos', { method: 'POST', body: JSON.stringify(body) });
    }
    mostrarProductosAdmin();
}

async function eliminarProducto(id, nombre) {
    if (!confirm('Eliminar "' + nombre + '"?')) return;
    await api('/api/productos/' + id, { method: 'DELETE' });
    mostrarProductosAdmin();
}

async function mostrarAdminPedidos() {
    const pedidos = await api('/api/pedidos');
    let html = '<h2>Gestion de Pedidos</h2>';
    pedidos.forEach(p => {
        html += `<div class="card">
            <p><strong>Pedido #${p.idPedido}</strong> | Cliente: ${p.idCliente} | $${p.total ? p.total.toLocaleString() : '0'}</p>
            <select id="est-${p.idPedido}">
                <option ${p.estado==='PENDIENTE'?'selected':''}>PENDIENTE</option>
                <option ${p.estado==='PAGADO'?'selected':''}>PAGADO</option>
                <option ${p.estado==='EN_PREPARACION'?'selected':''}>EN_PREPARACION</option>
                <option ${p.estado==='DESPACHADO'?'selected':''}>DESPACHADO</option>
                <option ${p.estado==='ENTREGADO'?'selected':''}>ENTREGADO</option>
                <option ${p.estado==='ANULADO'?'selected':''}>ANULADO</option>
            </select>
            <button onclick="cambiarEstado(${p.idPedido})">Actualizar</button>
            <button onclick="verDetallePedido(${p.idPedido})">Ver detalle</button>
            <button onclick="mostrarRecibo(${p.idPedido})">Ver recibo</button>
            <div id="detalle-${p.idPedido}"></div>
        </div>`;
    });
    render(html);
}

async function cambiarEstado(idPedido) {
    const estado = document.getElementById('est-' + idPedido).value;
    await api('/api/pedidos/' + idPedido + '/estado', { method: 'PUT', body: JSON.stringify({ estado }) });
    mostrarAdminPedidos();
}

async function mostrarLogs() {
    const logs = await api('/api/logs');
    let html = '<h2>Logs del Sistema</h2>';
    logs.forEach(l => {
        html += `<p style="font-size:12px;margin:2px 0">[${l.fechaHora}] ${l.tipo} — ${l.mensaje}</p>`;
    });
    render(html);
}

init();
