(function () {
    'use strict';

    const API = '/api/stories';
    const app = document.getElementById('app');

    // Spring Security stores the CSRF token in this cookie and expects it back in X-XSRF-TOKEN
    function csrfToken() {
        const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        return m ? decodeURIComponent(m[1]) : '';
    }

    async function request(url, options) {
        const headers = { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken() };
        const res = await fetch(url, Object.assign({ headers: headers }, options));
        if (res.status === 401) {
            // Session expired or never logged in
            location.href = '/login';
            return new Promise(() => {});
        }
        if (!res.ok) {
            let message = res.status + ' ' + res.statusText;
            try {
                const data = await res.json();
                if (data.message) message = data.message;
            } catch (e) { /* no JSON body */ }
            throw new Error(message);
        }
        return res.status === 204 ? null : res.json();
    }

    function render(templateId) {
        const node = document.getElementById(templateId).content.cloneNode(true);
        app.replaceChildren(node);
    }

    function showError(message) {
        app.innerHTML = '';
        const p = document.createElement('p');
        p.className = 'error';
        p.textContent = message;
        app.appendChild(p);
    }

    function excerpt(text, max) {
        const clean = text.replace(/\s+/g, ' ').trim();
        return clean.length > max ? clean.slice(0, max).trimEnd() + '…' : clean;
    }

    // ---- List ----
    async function listView() {
        render('tpl-list');
        const search = document.getElementById('search');
        let timer;
        search.addEventListener('input', () => {
            clearTimeout(timer);
            timer = setTimeout(() => loadList(search.value), 250);
        });
        await loadList('');
        search.focus();
    }

    async function loadList(query) {
        const list = document.getElementById('story-list');
        const empty = document.getElementById('empty');
        if (!list) return;
        const url = query.trim() ? API + '?q=' + encodeURIComponent(query.trim()) : API;
        const stories = await request(url);

        list.replaceChildren();
        stories.forEach(s => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.className = 'story-card';
            a.href = '#/story/' + s.id;

            const h2 = document.createElement('h2');
            h2.textContent = s.title;
            const meta = document.createElement('p');
            meta.className = 'meta';
            meta.textContent = 'by ' + s.author;
            const ex = document.createElement('p');
            ex.className = 'excerpt';
            ex.textContent = excerpt(s.body, 180);

            a.append(h2, meta, ex);
            li.appendChild(a);
            list.appendChild(li);
        });

        empty.hidden = stories.length > 0;
        empty.firstChild.textContent = query.trim() ? 'No stories match your search. ' : 'No stories yet. ';
    }

    // ---- Read ----
    let readObserver = null;

    function readCountText(story) {
        if (story.readCount === 0) return 'Not read yet';
        const times = 'Read ' + story.readCount + (story.readCount === 1 ? ' time' : ' times');
        const readers = ' by ' + story.readerCount + (story.readerCount === 1 ? ' reader' : ' readers');
        return times + readers;
    }

    async function storyView(id) {
        const story = await request(API + '/' + id);
        render('tpl-view');
        document.title = story.title + ' – Short Stories';
        app.querySelector('.story-title').textContent = story.title;
        app.querySelector('.story-author').textContent = 'by ' + story.author;
        app.querySelector('.story-reads').textContent = readCountText(story);
        app.querySelector('.story-body').textContent = story.body;
        app.querySelector('.edit-link').href = '#/edit/' + story.id;
        app.querySelector('.delete-btn').addEventListener('click', async () => {
            if (!confirm('Delete "' + story.title + '"? This cannot be undone.')) return;
            await request(API + '/' + story.id, { method: 'DELETE' });
            location.hash = '#/';
        });

        trackCompletedRead(story.id);
    }

    // Counts a "complete read" the moment the actions row (which sits right
    // after the body) scrolls into view - meaning the reader has scrolled past
    // the whole story, or it fit on screen without scrolling in the first place.
    // Fires at most once per view.
    function trackCompletedRead(id) {
        const actions = app.querySelector('.actions');
        if (!actions || typeof IntersectionObserver === 'undefined') return;
        readObserver = new IntersectionObserver((entries) => {
            if (!entries[0].isIntersecting) return;
            readObserver.disconnect();
            request(API + '/' + id + '/read', { method: 'POST' }).then(updated => {
                const reads = app.querySelector('.story-reads');
                if (reads) reads.textContent = readCountText(updated);
            }).catch(() => { /* non-essential */ });
        });
        readObserver.observe(actions);
    }

    // ---- Create / Edit ----
    async function formView(id) {
        const story = id ? await request(API + '/' + id) : null;
        // The author is fixed by the server: the story's author when editing, the logged-in user when creating
        const author = story ? story.author : (await request('/api/me').catch(() => ({}))).name || '';
        render('tpl-form');
        const form = app.querySelector('form');
        const error = form.querySelector('.error');
        form.querySelector('.form-heading').textContent = story ? 'Edit story' : 'New story';
        form.elements.author.value = author;
        if (story) {
            form.elements.title.value = story.title;
            form.elements.body.value = story.body;
            form.querySelector('.cancel-link').href = '#/story/' + story.id;
        }
        form.elements.title.focus();

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                title: form.elements.title.value.trim(),
                body: form.elements.body.value.trim()
            };
            if (!payload.title || !payload.body) {
                error.textContent = 'Title and story are required.';
                error.hidden = false;
                return;
            }
            try {
                const saved = await request(story ? API + '/' + story.id : API, {
                    method: story ? 'PUT' : 'POST',
                    body: JSON.stringify(payload)
                });
                location.hash = '#/story/' + saved.id;
            } catch (err) {
                error.textContent = 'Could not save: ' + err.message;
                error.hidden = false;
            }
        });
    }

    // ---- Router ----
    async function route() {
        if (readObserver) {
            readObserver.disconnect();
            readObserver = null;
        }
        document.title = 'Short Stories';
        const hash = location.hash.replace(/^#/, '') || '/';
        let m;
        try {
            if (hash === '/') {
                await listView();
            } else if (hash === '/new') {
                await formView(null);
            } else if ((m = hash.match(/^\/story\/(\d+)$/))) {
                await storyView(m[1]);
            } else if ((m = hash.match(/^\/edit\/(\d+)$/))) {
                await formView(m[1]);
            } else {
                showError('Page not found.');
            }
        } catch (err) {
            showError(err.message);
        }
    }

    // ---- Session ----
    document.getElementById('logout-btn').addEventListener('click', async () => {
        await fetch('/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrfToken() } });
        location.href = '/login?logout';
    });

    request('/api/me').then(me => {
        document.getElementById('user-email').textContent = me.email;
    }).catch(() => { /* non-essential */ });

    window.addEventListener('hashchange', route);
    route();
})();
