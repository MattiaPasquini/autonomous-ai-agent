const form = document.getElementById('chat-form');
const input = document.getElementById('prompt-input');
const sendBtn = document.getElementById('send-btn');
const messages = document.getElementById('messages');

const sandboxModal = document.getElementById('sandbox-modal');
const sandboxForm = document.getElementById('sandbox-form');
const sandboxInput = document.getElementById('sandbox-input');
const sandboxError = document.getElementById('sandbox-error');
const sandboxBtn = document.getElementById('sandbox-btn');

let pendingConfirmation = false;

marked.setOptions({ breaks: true, gfm: true });

input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 120) + 'px';
});

input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        form.requestSubmit();
    }
});

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (pendingConfirmation) return;

    const text = input.value.trim();
    if (!text) return;

    appendMessage('user', text);
    input.value = '';
    input.style.height = 'auto';
    setLoading(true);

    const typing = appendTyping();

    try {
        const response = await postJSON('/api/ai/ask', { prompt: text });

        typing.remove();

        if (response.ok) {
            handleChatResponse(await response.json());
        } else {
            const data = await response.json();
            setLoading(false);
            appendMessage('assistant', data.detail || 'Something went wrong while sending a message');
        }


    } catch (err) {
        typing.remove();
        appendMessage('assistant', `Errore: ${err.message}`);
        setLoading(false);
    }
});

function handleChatResponse(chatResponse) {
    const { message, status } = chatResponse;

    if (status === 'CONFIRMATION_NEEDED') {
        appendConfirmation(message);
    } else {
        appendMessage('assistant', message);
        setLoading(false);
    }
}

async function postJSON(url, body) {
    return fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

function renderMarkdown(text) {
    return DOMPurify.sanitize(marked.parse(text));
}

function appendMessage(role, text) {
    const msg = document.createElement('div');
    msg.className = `message ${role}`;

    const bubble = document.createElement('div');
    bubble.className = 'bubble';

    if (role === 'assistant') {
        bubble.classList.add('markdown-body');
        bubble.innerHTML = renderMarkdown(text);
    } else {
        bubble.textContent = text; // utente: sempre testo semplice, mai HTML
    }

    msg.appendChild(bubble);
    messages.appendChild(msg);
    scrollToBottom();
    return msg;
}

function appendSystemLabel(text) {
    const label = document.createElement('div');
    label.className = 'system-label';
    label.textContent = text;
    messages.appendChild(label);
    scrollToBottom();
    return label;
}

function appendTyping() {
    const msg = document.createElement('div');
    msg.className = 'message assistant typing';
    msg.innerHTML = '<div class="bubble"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>';
    messages.appendChild(msg);
    scrollToBottom();
    return msg;
}

function appendConfirmation(text) {
    pendingConfirmation = true;
    setLoading(true);

    const msg = document.createElement('div');
    msg.className = 'message assistant';

    const bubble = document.createElement('div');
    bubble.className = 'bubble confirmation-bubble';

    const textEl = document.createElement('div');
    textEl.className = 'confirmation-text markdown-body';
    textEl.innerHTML = renderMarkdown(text);

    const actions = document.createElement('div');
    actions.className = 'confirmation-actions';

    const makeBtn = (label, className) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = `confirm-btn ${className}`;
        btn.textContent = label;
        return btn;
    };

    const confirmBtn = makeBtn('Conferma', 'confirm-yes');
    const cancelBtn = makeBtn('Annulla', 'confirm-no');

    confirmBtn.addEventListener('click', () => resolveConfirmation(true, actions));
    cancelBtn.addEventListener('click', () => resolveConfirmation(false, actions));

    actions.append(confirmBtn, cancelBtn);
    bubble.append(textEl, actions);
    msg.appendChild(bubble);
    messages.appendChild(msg);
    scrollToBottom();
}

async function resolveConfirmation(confirmed, actionsEl) {
    actionsEl.querySelectorAll('button').forEach(b => b.disabled = true);
    actionsEl.classList.add('resolved');

    const result = document.createElement('div');
    result.className = 'confirmation-result';
    result.textContent = confirmed ? '✓ Confermato' : '✗ Annullato';
    actionsEl.appendChild(result);

    const typing = appendTyping();

    try {
        const response = await postJSON('/api/ai/confirm', { confirmation: confirmed });
        typing.remove();
        pendingConfirmation = false;

        if (response.ok) {
            handleChatResponse(await response.json());
        } else {
            const data = await response.json();
            appendMessage('assistant', data.detail || 'Something went wrong');
            setLoading(false);
        }
    } catch (err) {
        typing.remove();
        pendingConfirmation = false;
        appendMessage('assistant', `Errore: ${err.message}`);
        setLoading(false);
    }
}

function setLoading(loading) {
    sendBtn.disabled = loading;
    input.disabled = loading;
}

function scrollToBottom() {
    messages.scrollTop = messages.scrollHeight;
}


sandboxForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const path = sandboxInput.value.trim();
    if (!path) return;

    sandboxBtn.disabled = true;
    sandboxError.textContent = '';

    try {
        const response = await postJSON('/api/sandbox/new', { path });

        if (response.ok) {
            sandboxModal.classList.add('hidden');
            setLoading(false);
            appendSystemLabel(`Sandbox folder set to: ${path}`);
        } else {
            const data = await response.json();
            sandboxError.textContent = data.detail || 'Failed to set sandbox';
        }
    } catch (err) {
        sandboxError.textContent = 'Connection error: ' + err.message;
    } finally {
        sandboxBtn.disabled = false;
    }
});

async function initSandbox() {
    try {
        const response = await fetch('/api/sandbox');
        const data = await response.json();
        if (data.path !== null) {
            sandboxModal.classList.add('hidden');
            setLoading(false);
        }
    } catch {
        sandboxModal.classList.add('hidden');
        setLoading(false);
    }
}
initSandbox();
