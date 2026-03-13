import './style.css'

const topicListEl = document.getElementById('topic-list') as HTMLUListElement;
const topicLoaderEl = document.getElementById('topic-loader') as HTMLDivElement;
const messageInputEl = document.getElementById('message-input') as HTMLInputElement;
const sendBtnEl = document.getElementById('send-btn') as HTMLButtonElement;
const fileInputEl = document.getElementById('file-input') as HTMLInputElement;
const uploadBtnEl = document.getElementById('upload-btn') as HTMLButtonElement;
const toastEl = document.getElementById('toast') as HTMLDivElement;
const messagesContainerEl = document.getElementById('messages-container') as HTMLDivElement;
const connectionStatusEl = document.getElementById('connection-status') as HTMLSpanElement;
const noMessagesMsgEl = document.getElementById('no-messages-msg') as HTMLDivElement;
const notifBellEl = document.getElementById('notif-bell') as HTMLDivElement;
const notifBadgeEl = document.getElementById('notif-badge') as HTMLDivElement;

let notificationCount = 0;

const API_BASE = 'http://localhost:8080/api';

function showToast(message: string, isError: boolean = false) {
  toastEl.textContent = message;
  toastEl.className = `toast-${isError ? 'error' : 'success'} show`;
  setTimeout(() => {
    toastEl.className = '';
  }, 3000);
}

// ...fetchTopics... 
async function fetchTopics() {
  try {
    topicLoaderEl.style.display = 'block';
    
    // Fetch topics from our backend KafkaTopicController
    const res = await fetch(`${API_BASE}/kafka/topics`);
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    
    const topics: string[] = await res.json();
    
    topicLoaderEl.style.display = 'none';
    topicListEl.innerHTML = '';
    
    if (topics.length === 0) {
      topicListEl.innerHTML = '<li class="topic-item" style="color: var(--text-muted); justify-content: center;">No topics found.</li>';
      return;
    }
    
    topics.forEach((topic) => {
      // Exclude internal topics usually starting with underscores if you prefer, but we return all.
      const isInternal = topic.startsWith('__');
      
      const li = document.createElement('li');
      li.className = 'topic-item';
      li.innerHTML = `
        <div>
          <span style="font-weight: 500; color: ${isInternal ? 'var(--text-muted)' : 'var(--text)'}">${topic}</span>
        </div>
        <span class="status-badge">Active</span>
      `;
      topicListEl.appendChild(li);
    });
    
  } catch (error: any) {
    topicLoaderEl.style.display = 'none';
    topicListEl.innerHTML = `<li class="topic-item" style="color: var(--error); justify-content: center;">Failed to load topics: ${error.message}</li>`;
    showToast('Failed to connect to backend', true);
  }
}

async function sendMessage() {
  const message = messageInputEl.value.trim();
  if (!message) {
    showToast('Please enter a message.', true);
    return;
  }
  
  sendBtnEl.disabled = true;
  sendBtnEl.textContent = 'Sending...';
  
  try {
    const formData = new URLSearchParams();
    formData.append('message', message);
    
    const res = await fetch(`${API_BASE}/bizg/produce`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: formData
    });
    
    if (!res.ok) throw new Error(`Failed to send message: ${res.status}`);
    
    const data = await res.text();
    showToast(data);
    messageInputEl.value = '';
    
  } catch (error: any) {
    showToast(error.message, true);
  } finally {
    sendBtnEl.disabled = false;
    sendBtnEl.textContent = 'Send Message';
  }
}

async function uploadFile() {
  const files = fileInputEl.files;
  if (!files || files.length === 0) {
    showToast('Please select a file to upload.', true);
    return;
  }
  
  const file = files[0];
  uploadBtnEl.disabled = true;
  uploadBtnEl.textContent = 'Uploading...';
  
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const res = await fetch(`${API_BASE}/bizg/produce/file`, {
      method: 'POST',
      body: formData
    });
    
    if (!res.ok) throw new Error(`Failed to upload file: ${res.status}`);
    
    const data = await res.text();
    showToast(data);
    fileInputEl.value = '';
    
  } catch (error: any) {
    showToast(error.message, true);
  } finally {
    uploadBtnEl.disabled = false;
    uploadBtnEl.textContent = 'Upload File';
  }
}

function setupEventSource() {
  const evtSource = new EventSource(`${API_BASE}/stream/messages`);

  evtSource.onopen = () => {
    connectionStatusEl.textContent = 'Listening';
    connectionStatusEl.style.background = 'rgba(16, 185, 129, 0.2)';
    connectionStatusEl.style.color = '#34d399';
  };

  evtSource.addEventListener('message', (event) => {
    if(noMessagesMsgEl) noMessagesMsgEl.style.display = 'none';

    // Update Notification Count
    notificationCount++;
    notifBadgeEl.textContent = notificationCount.toString();
    notifBadgeEl.classList.add('show');
    
    // Add ringing effect
    notifBellEl.classList.remove('ring');
    void notifBellEl.offsetWidth; // trigger reflow
    notifBellEl.classList.add('ring');

    const data = event.data;
    const msgEl = document.createElement('div');
    msgEl.className = 'topic-item';
    msgEl.style.padding = '0.75rem 1rem';
    msgEl.style.display = 'flex';
    msgEl.innerHTML = `
      <div style="display:flex; align-items:center; gap: 0.5rem; flex: 1;">
        <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:var(--primary);"></span>
        <span style="color: var(--text);">${data}</span>
      </div>
      <span style="font-size: 0.75rem; color:var(--text-muted);">${new Date().toLocaleTimeString()}</span>
    `;
    
    messagesContainerEl.prepend(msgEl);
  });

  evtSource.onerror = () => {
    connectionStatusEl.textContent = 'Disconnected';
    connectionStatusEl.style.background = 'rgba(239, 68, 68, 0.2)';
    connectionStatusEl.style.color = '#f87171';
  };
}

// Event Listeners
sendBtnEl.addEventListener('click', sendMessage);
uploadBtnEl.addEventListener('click', uploadFile);
notifBellEl.addEventListener('click', () => {
  notificationCount = 0;
  notifBadgeEl.classList.remove('show');
  notifBellEl.classList.remove('ring');
});
messageInputEl.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    sendMessage();
  }
});

// Init
fetchTopics();
setupEventSource();
