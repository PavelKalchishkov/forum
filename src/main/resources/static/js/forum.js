const TOKEN_KEY = 'forum_token';

const app = document.getElementById('app');
const headerActions = document.getElementById('header-actions');
const authModal = document.getElementById('auth-modal');
const topicModal = document.getElementById('topic-modal');
const toastEl = document.getElementById('toast');

let toastTimer;

const ROLE_LABELS = {
	USER: 'Потребител',
	MODERATOR: 'Модератор',
	ADMIN: 'Администратор',
};

// --- Auth ---

function getToken() {
	return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
	if (token) localStorage.setItem(TOKEN_KEY, token);
	else localStorage.removeItem(TOKEN_KEY);
}

function parseJwt(token) {
	try {
		const payload = token.split('.')[1];
		return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
	} catch {
		return null;
	}
}

function getCurrentUser() {
	const token = getToken();
	if (!token) return null;
	const claims = parseJwt(token);
	if (!claims || (claims.exp && claims.exp * 1000 < Date.now())) {
		setToken(null);
		return null;
	}
	return { id: claims.uid, username: claims.sub, role: claims.role };
}

function canManage(authorId) {
	const user = getCurrentUser();
	if (!user) return false;
	if (user.role === 'ADMIN' || user.role === 'MODERATOR') return true;
	return user.id === authorId;
}

function isAdmin() {
	return getCurrentUser()?.role === 'ADMIN';
}

// --- API ---

async function api(path, options = {}) {
	const headers = { ...(options.headers || {}) };
	if (options.body && !headers['Content-Type']) {
		headers['Content-Type'] = 'application/json';
	}
	const token = getToken();
	if (token) headers['Authorization'] = `Bearer ${token}`;

	const res = await fetch(path, { ...options, headers });
	if (!res.ok) {
		const err = new Error(await res.text() || res.statusText);
		err.status = res.status;
		throw err;
	}
	if (res.status === 204) return null;
	const text = await res.text();
	return text ? JSON.parse(text) : null;
}

// --- UI helpers ---

function showToast(msg, isError = false) {
	toastEl.textContent = msg;
	toastEl.classList.toggle('error', isError);
	toastEl.classList.remove('hidden');
	clearTimeout(toastTimer);
	toastTimer = setTimeout(() => toastEl.classList.add('hidden'), 3500);
}

function fmtDate(iso) {
	if (!iso) return '—';
	return new Date(iso).toLocaleString('bg-BG', {
		day: '2-digit', month: '2-digit', year: 'numeric',
		hour: '2-digit', minute: '2-digit',
	});
}

function esc(str) {
	const d = document.createElement('div');
	d.textContent = str ?? '';
	return d.innerHTML;
}

function userLink(id, username) {
	if (!id || !username) return esc(username ?? '—');
	return `<a href="#/user/${id}" class="user-link">${esc(username)}</a>`;
}

function roleBadge(role) {
	const cls = role === 'ADMIN' ? 'admin' : role === 'MODERATOR' ? 'moderator' : '';
	return `<span class="role-badge ${cls}">${esc(ROLE_LABELS[role] || role)}</span>`;
}

function renderHeader() {
	const user = getCurrentUser();
	if (user) {
		headerActions.innerHTML = `
			<span class="user-badge">Здравей, ${userLink(user.id, user.username)} ${roleBadge(user.role)}</span>
			<button class="btn btn-primary btn-sm" id="btn-new-topic">+ Нова тема</button>
			<button class="btn btn-ghost btn-sm" id="btn-logout">Изход</button>`;
		document.getElementById('btn-new-topic').onclick = () => topicModal.classList.remove('hidden');
		document.getElementById('btn-logout').onclick = () => { setToken(null); renderHeader(); navigate(); };
	} else {
		headerActions.innerHTML = `
			<button class="btn btn-ghost btn-sm" id="btn-login">Вход</button>
			<button class="btn btn-primary btn-sm" id="btn-register">Регистрация</button>`;
		document.getElementById('btn-login').onclick = () => openAuth('login');
		document.getElementById('btn-register').onclick = () => openAuth('register');
	}
}

function openAuth(tab) {
	authModal.classList.remove('hidden');
	switchAuthTab(tab);
}

function switchAuthTab(tab) {
	const loginForm = document.getElementById('login-form');
	const registerForm = document.getElementById('register-form');
	const tabs = authModal.querySelectorAll('.modal-tabs button');
	tabs.forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
	loginForm.classList.toggle('hidden', tab !== 'login');
	registerForm.classList.toggle('hidden', tab !== 'register');
	document.getElementById('auth-modal-title').textContent = tab === 'login' ? 'Вход' : 'Регистрация';
}

function manageActions(type, id, authorId, extra = '') {
	if (!canManage(authorId)) return '';
	const editUrl = type === 'post'
		? `#/topic/${id}/edit`
		: `#/reply/${id}/edit${extra}`;
	return `<div class="action-bar">
		<a class="btn btn-ghost btn-sm" href="${editUrl}">Редактирай</a>
		<button type="button" class="btn btn-danger btn-sm" data-delete-${type}="${id}">Изтрий</button>
	</div>`;
}

// --- Views ---

async function renderTopicList() {
	app.innerHTML = '<p class="page-title">Теми</p><div class="panel" id="topic-list"><div class="empty-state">Зареждане…</div></div>';
	try {
		const posts = await api('/posts');
		const panel = document.getElementById('topic-list');
		if (!posts.length) {
			panel.innerHTML = '<div class="empty-state">Няма теми. Бъди първи!</div>';
			return;
		}
		panel.innerHTML = `
			<table class="topic-table">
				<thead>
					<tr>
						<th>Тема</th>
						<th class="col-narrow">Прегледи</th>
						<th class="col-meta">Автор</th>
						<th class="col-meta">Дата</th>
					</tr>
				</thead>
				<tbody>
					${posts.map(p => `
						<tr>
							<td><a class="topic-title-link" href="#/topic/${p.id}">${esc(p.title)}</a></td>
							<td class="col-narrow">${p.viewCount ?? 0}</td>
							<td class="col-meta">${userLink(p.author?.id, p.author?.username)}</td>
							<td class="col-meta">${fmtDate(p.updatedAt || p.createdAt)}</td>
						</tr>`).join('')}
				</tbody>
			</table>`;
	} catch (e) {
		document.getElementById('topic-list').innerHTML = `<div class="empty-state">Грешка: ${esc(e.message)}</div>`;
	}
}

async function renderTopicDetail(id, page = 0) {
	app.innerHTML = '<div class="empty-state">Зареждане…</div>';
	try {
		const [post, repliesPage] = await Promise.all([
			api(`/posts/${id}`),
			api(`/posts/${id}/replies?page=${page}&size=10`),
		]);

		const user = getCurrentUser();
		const authorId = post.author?.id;

		app.innerHTML = `
			<a href="#/" class="back-link">← Към темите</a>
			<div class="panel">
				<div class="topic-header">
					<h1>${esc(post.title)}</h1>
					<div class="topic-meta">
						<span>Автор: ${userLink(post.author?.id, post.author?.username)}</span>
						<span>Създадена: ${fmtDate(post.createdAt)}</span>
						<span>Обновена: ${fmtDate(post.updatedAt)}</span>
						<span>Прегледи: ${post.viewCount ?? 0}</span>
					</div>
					${manageActions('post', post.id, authorId)}
				</div>
				<div class="topic-body">${esc(post.content)}</div>

				<div class="replies-header">Отговори (${repliesPage.totalElements})</div>
				<div id="replies-list">
					${repliesPage.content.length ? repliesPage.content.map(r => renderReply(r, id, page)).join('') : '<div class="empty-state">Няма отговори</div>'}
				</div>
				${renderPagination(id, repliesPage)}
				${user ? renderReplyForm() : `<div class="form-panel"><p style="margin:0;color:var(--muted);font-size:0.875rem"><a href="#" id="login-to-reply">Влез</a>, за да отговориш.</p></div>`}
			</div>`;

		bindDeleteButtons('post', () => location.hash = '#/');
		bindDeleteButtons('reply', () => renderTopicDetail(id, page));

		if (!user) {
			document.getElementById('login-to-reply')?.addEventListener('click', e => { e.preventDefault(); openAuth('login'); });
		} else {
			document.getElementById('reply-form')?.addEventListener('submit', e => submitReply(e, id, page));
		}
	} catch (e) {
		app.innerHTML = `<a href="#/" class="back-link">← Към темите</a><div class="empty-state">${e.status === 404 ? 'Темата не е намерена.' : `Грешка: ${esc(e.message)}`}</div>`;
	}
}

function renderReply(r, topicId, page) {
	const extra = `?topic=${topicId}&page=${page}`;
	return `
		<div class="reply" id="reply-${r.id}">
			<div class="reply-author">
				<span class="name">${userLink(r.author?.id, r.author?.username)}</span>
				<span class="date">${fmtDate(r.createdAt)}</span>
				${r.updatedAt !== r.createdAt ? `<span class="date">ред. ${fmtDate(r.updatedAt)}</span>` : ''}
			</div>
			<div class="reply-content">
				<div class="reply-text">${esc(r.content)}</div>
				${manageActions('reply', r.id, r.author?.id, extra)}
			</div>
		</div>`;
}

function renderPagination(topicId, page) {
	if (page.totalPages <= 1) return '';
	let btns = '';
	if (page.page > 0) btns += `<a class="btn btn-ghost btn-sm" href="#/topic/${topicId}?page=${page.page - 1}">← Предишна</a>`;
	btns += `<span>Страница ${page.page + 1} / ${page.totalPages}</span>`;
	if (page.page < page.totalPages - 1) btns += `<a class="btn btn-ghost btn-sm" href="#/topic/${topicId}?page=${page.page + 1}">Следваща →</a>`;
	return `<div class="pagination">${btns}</div>`;
}

function renderReplyForm() {
	return `
		<div class="form-panel">
			<h3>Напиши отговор</h3>
			<form id="reply-form">
				<div class="form-group">
					<textarea name="content" required maxlength="10000" placeholder="Твоят отговор…"></textarea>
				</div>
				<div class="form-actions">
					<button type="submit" class="btn btn-primary">Публикувай</button>
				</div>
			</form>
		</div>`;
}

async function renderEditPost(id) {
	app.innerHTML = '<div class="empty-state">Зареждане…</div>';
	try {
		const post = await api(`/posts/${id}?trackView=false`);
		if (!canManage(post.author?.id)) {
			app.innerHTML = `<a href="#/topic/${id}" class="back-link">← Назад</a><div class="empty-state">Нямаш права да редактираш тази тема.</div>`;
			return;
		}
		app.innerHTML = `
			<a href="#/topic/${id}" class="back-link">← Към темата</a>
			<div class="edit-page">
				<div class="panel">
					<h1>Редактиране на тема</h1>
					<form id="edit-post-form">
						<div class="form-group">
							<label for="edit-title">Заглавие</label>
							<input id="edit-title" name="title" required maxlength="500" value="${esc(post.title)}">
						</div>
						<div class="form-group">
							<label for="edit-content">Съдържание</label>
							<textarea id="edit-content" name="content" required maxlength="10000">${esc(post.content)}</textarea>
						</div>
						<div class="form-actions">
							<button type="submit" class="btn btn-primary">Запази</button>
							<a href="#/topic/${id}" class="btn btn-ghost">Отказ</a>
							<button type="button" class="btn btn-danger" id="delete-post-btn">Изтрий темата</button>
						</div>
					</form>
				</div>
			</div>`;

		document.getElementById('edit-post-form').onsubmit = async e => {
			e.preventDefault();
			const fd = new FormData(e.target);
			try {
				await api(`/posts/${id}`, {
					method: 'PUT',
					body: JSON.stringify({ title: fd.get('title').trim(), content: fd.get('content').trim() }),
				});
				showToast('Темата е обновена');
				location.hash = `#/topic/${id}`;
			} catch (err) {
				showToast(err.status === 409 ? 'Заглавието вече съществува.' : 'Грешка при запис.', true);
			}
		};
		document.getElementById('delete-post-btn').onclick = () => deletePost(id);
	} catch (e) {
		app.innerHTML = `<a href="#/" class="back-link">← Към темите</a><div class="empty-state">${e.status === 404 ? 'Темата не е намерена.' : `Грешка: ${esc(e.message)}`}</div>`;
	}
}

async function renderEditReply(id, topicId, page) {
	app.innerHTML = '<div class="empty-state">Зареждане…</div>';
	try {
		const reply = await api(`/replies/${id}`);
		if (!canManage(reply.author?.id)) {
			app.innerHTML = `<a href="#/topic/${topicId}?page=${page}" class="back-link">← Назад</a><div class="empty-state">Нямаш права да редактираш този отговор.</div>`;
			return;
		}
		const back = `#/topic/${topicId}?page=${page}`;
		app.innerHTML = `
			<a href="${back}" class="back-link">← Към темата</a>
			<div class="edit-page">
				<div class="panel">
					<h1>Редактиране на отговор</h1>
					<form id="edit-reply-form">
						<div class="form-group">
							<label for="edit-reply-content">Съдържание</label>
							<textarea id="edit-reply-content" name="content" required maxlength="10000">${esc(reply.content)}</textarea>
						</div>
						<div class="form-actions">
							<button type="submit" class="btn btn-primary">Запази</button>
							<a href="${back}" class="btn btn-ghost">Отказ</a>
							<button type="button" class="btn btn-danger" id="delete-reply-btn">Изтрий отговора</button>
						</div>
					</form>
				</div>
			</div>`;

		document.getElementById('edit-reply-form').onsubmit = async e => {
			e.preventDefault();
			const content = e.target.content.value.trim();
			try {
				await api(`/replies/${id}`, { method: 'PUT', body: JSON.stringify({ content }) });
				showToast('Отговорът е обновен');
				location.hash = back;
			} catch {
				showToast('Грешка при запис.', true);
			}
		};
		document.getElementById('delete-reply-btn').onclick = () => deleteReply(id, topicId, page);
	} catch (e) {
		app.innerHTML = `<a href="#/" class="back-link">← Към темите</a><div class="empty-state">${e.status === 404 ? 'Отговорът не е намерен.' : `Грешка: ${esc(e.message)}`}</div>`;
	}
}

async function renderUserProfile(id) {
	app.innerHTML = '<div class="empty-state">Зареждане…</div>';
	try {
		const profile = await api(`/users/${id}`);
		const current = getCurrentUser();
		const isOwn = current && current.id === profile.id;
		const adminView = isAdmin() && !isOwn;

		app.innerHTML = `
			<a href="#/" class="back-link">← Към темите</a>
			<div class="panel profile-card">
				<h1>${esc(profile.username)} ${roleBadge(profile.role)}</h1>
				<div class="profile-meta">
					${profile.email ? `<p>Имейл: ${esc(profile.email)}</p>` : ''}
					<p>Регистриран: ${fmtDate(profile.createdAt)}</p>
					${isOwn ? '<p><em>Това е твоят профил.</em></p>' : ''}
				</div>
				${adminView ? renderAdminUserActions(profile) : ''}
			</div>`;

		if (adminView) bindAdminUserActions(profile);
	} catch (e) {
		app.innerHTML = `<a href="#/" class="back-link">← Към темите</a><div class="empty-state">${e.status === 404 ? 'Потребителят не е намерен.' : `Грешка: ${esc(e.message)}`}</div>`;
	}
}

function renderAdminUserActions(profile) {
	return `
		<div class="admin-actions">
			<h3>Администраторски действия</h3>
			<p style="margin:0 0 0.75rem;font-size:0.875rem;color:var(--muted)">Промени ролята на потребителя:</p>
			<div class="action-bar">
				<button class="btn btn-ghost btn-sm" data-set-role="USER" ${profile.role === 'USER' ? 'disabled' : ''}>Потребител</button>
				<button class="btn btn-ghost btn-sm" data-set-role="MODERATOR" ${profile.role === 'MODERATOR' ? 'disabled' : ''}>Модератор</button>
				<button class="btn btn-ghost btn-sm" data-set-role="ADMIN" ${profile.role === 'ADMIN' ? 'disabled' : ''}>Администратор</button>
			</div>
			<div class="action-bar" style="margin-top:1rem">
				<button class="btn btn-danger btn-sm" id="delete-user-btn">Изтрий потребителя</button>
			</div>
		</div>`;
}

function bindAdminUserActions(profile) {
	document.querySelectorAll('[data-set-role]').forEach(btn => {
		btn.onclick = async () => {
			const role = btn.dataset.setRole;
			if (!confirm(`Сигурен ли си, че искаш да направиш ${profile.username} → ${ROLE_LABELS[role]}?`)) return;
			try {
				await api(`/users/${profile.id}`, {
					method: 'PUT',
					body: JSON.stringify({ username: profile.username, email: profile.email, role }),
				});
				showToast('Ролята е обновена');
				renderUserProfile(profile.id);
			} catch {
				showToast('Грешка при промяна на роля.', true);
			}
		};
	});
	document.getElementById('delete-user-btn').onclick = async () => {
		if (!confirm(`Сигурен ли си, че искаш да изтриеш ${profile.username}?`)) return;
		try {
			await api(`/users/${profile.id}`, { method: 'DELETE' });
			showToast('Потребителят е изтрит');
			location.hash = '#/';
		} catch {
			showToast('Грешка при изтриване.', true);
		}
	};
}

// --- Actions ---

async function submitReply(e, postId, page) {
	e.preventDefault();
	const content = e.target.content.value.trim();
	if (!content) return;
	try {
		await api(`/posts/${postId}/replies`, { method: 'POST', body: JSON.stringify({ content }) });
		showToast('Отговорът е публикуван');
		renderTopicDetail(postId, page);
	} catch (err) {
		showToast(err.status === 403 ? 'Нямаш права.' : 'Грешка при публикуване.', true);
	}
}

async function deletePost(id) {
	if (!confirm('Сигурен ли си, че искаш да изтриеш тази тема?')) return;
	try {
		await api(`/posts/${id}`, { method: 'DELETE' });
		showToast('Темата е изтрита');
		location.hash = '#/';
	} catch (err) {
		showToast(err.status === 403 ? 'Нямаш права.' : 'Грешка при изтриване.', true);
	}
}

async function deleteReply(id, topicId, page) {
	if (!confirm('Сигурен ли си, че искаш да изтриеш този отговор?')) return;
	try {
		await api(`/replies/${id}`, { method: 'DELETE' });
		showToast('Отговорът е изтрит');
		location.hash = `#/topic/${topicId}?page=${page}`;
	} catch (err) {
		showToast(err.status === 403 ? 'Нямаш права.' : 'Грешка при изтриване.', true);
	}
}

function bindDeleteButtons(type, onSuccess) {
	document.querySelectorAll(`[data-delete-${type}]`).forEach(btn => {
		btn.onclick = async () => {
			const id = btn.dataset[`delete${type.charAt(0).toUpperCase() + type.slice(1)}`];
			if (!confirm(`Сигурен ли си, че искаш да изтриеш този ${type === 'post' ? 'тема' : 'отговор'}?`)) return;
			try {
				await api(`/${type === 'post' ? 'posts' : 'replies'}/${id}`, { method: 'DELETE' });
				showToast('Изтрито');
				onSuccess();
			} catch (err) {
				showToast(err.status === 403 ? 'Нямаш права.' : 'Грешка при изтриване.', true);
			}
		};
	});
}

// --- Routing ---

function parseRoute() {
	const hash = location.hash.slice(1) || '/';
	const parts = hash.split('?');
	const path = parts[0];
	const params = new URLSearchParams(parts[1] || '');

	const editPost = path.match(/^\/topic\/(\d+)\/edit$/);
	if (editPost) return { view: 'editPost', id: editPost[1] };

	const editReply = path.match(/^\/reply\/(\d+)\/edit$/);
	if (editReply) return { view: 'editReply', id: editReply[1], topicId: params.get('topic'), page: Number(params.get('page') || 0) };

	const topic = path.match(/^\/topic\/(\d+)$/);
	if (topic) return { view: 'topic', id: topic[1], page: Number(params.get('page') || 0) };

	const user = path.match(/^\/user\/(\d+)$/);
	if (user) return { view: 'user', id: user[1] };

	return { view: 'list' };
}

function navigate() {
	const route = parseRoute();
	switch (route.view) {
		case 'editPost': renderEditPost(route.id); break;
		case 'editReply': renderEditReply(route.id, route.topicId, route.page); break;
		case 'topic': renderTopicDetail(route.id, route.page); break;
		case 'user': renderUserProfile(route.id); break;
		default: renderTopicList();
	}
}

// --- Init ---

document.getElementById('login-form').addEventListener('submit', async e => {
	e.preventDefault();
	const fd = new FormData(e.target);
	try {
		const res = await api('/auth/login', {
			method: 'POST',
			body: JSON.stringify({ username: fd.get('username'), password: fd.get('password') }),
		});
		setToken(res.accessToken);
		authModal.classList.add('hidden');
		showToast('Успешен вход');
		renderHeader();
		navigate();
	} catch {
		showToast('Грешно потребителско име или парола.', true);
	}
});

document.getElementById('register-form').addEventListener('submit', async e => {
	e.preventDefault();
	const fd = new FormData(e.target);
	const body = { username: fd.get('username'), password: fd.get('password') };
	const email = fd.get('email')?.toString().trim();
	if (email) body.email = email;
	try {
		await api('/auth/register', { method: 'POST', body: JSON.stringify(body) });
		const res = await api('/auth/login', {
			method: 'POST',
			body: JSON.stringify({ username: body.username, password: body.password }),
		});
		setToken(res.accessToken);
		authModal.classList.add('hidden');
		showToast('Регистрацията е успешна');
		renderHeader();
		navigate();
	} catch (err) {
		showToast(err.status === 409 ? 'Потребителят вече съществува.' : 'Грешка при регистрация.', true);
	}
});

document.getElementById('new-topic-form').addEventListener('submit', async e => {
	e.preventDefault();
	const fd = new FormData(e.target);
	try {
		const post = await api('/posts', {
			method: 'POST',
			body: JSON.stringify({ title: fd.get('title').trim(), content: fd.get('content').trim() }),
		});
		topicModal.classList.add('hidden');
		e.target.reset();
		showToast('Темата е публикувана');
		location.hash = `#/topic/${post.id}`;
	} catch (err) {
		showToast(err.status === 409 ? 'Заглавието вече съществува.' : 'Грешка при публикуване.', true);
	}
});

authModal.querySelectorAll('.modal-tabs button').forEach(btn => {
	btn.onclick = () => switchAuthTab(btn.dataset.tab);
});
document.getElementById('close-auth').onclick = () => authModal.classList.add('hidden');
document.getElementById('close-auth-2').onclick = () => authModal.classList.add('hidden');
document.getElementById('close-topic').onclick = () => topicModal.classList.add('hidden');
authModal.addEventListener('click', e => { if (e.target === authModal) authModal.classList.add('hidden'); });
topicModal.addEventListener('click', e => { if (e.target === topicModal) topicModal.classList.add('hidden'); });

window.addEventListener('hashchange', navigate);
renderHeader();
navigate();
