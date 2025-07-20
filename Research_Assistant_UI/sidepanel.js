document.addEventListener('DOMContentLoaded', () => {
    chrome.storage.local.get(['researchNotes'], function(result){
        if(result.researchNotes) {
            document.getElementById('notes').value = result.researchNotes;
        }
    });

    document.getElementById('summarizeBtn').addEventListener('click', summarizeText);
    document.getElementById('saveNotesBtn').addEventListener('click', saveNotes);
    document.getElementById('darkModeToggle').addEventListener('click', toggleDarkMode);

    // Load saved dark mode preference
    chrome.storage.local.get('darkMode', (result) => {
        if (result.darkMode) {
            document.body.classList.add('dark-mode');
        }
    });
});


async function summarizeText() {
    try {
        showLoader(true);
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        const [{ result }] = await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            function: () => window.getSelection().toString()
        });

        if (!result) {
            showLoader(false);
            showResult('Please select some text to summarize.');
            return;
        }

        const response = await fetch('http://localhost:8080/api/research/process', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: result, operation: 'summarize' })
        });

        if (!response.ok) {
            throw new Error(`API Error: ${response.status}`);
        }

        const text = await response.text();
        showLoader(false);
        showResult(text.replace(/\n/g, '<br>'));
    } catch (error) {
        showLoader(false);
        showResult('Error: ' + error.message);
    }
}

function saveNotes() {
    const notes = document.getElementById('notes').value;
    chrome.storage.local.set({ 'researchNotes': notes }, function () {
        alert('Notes saved successfully.');
    });
    showResult('Notes saved successfully.');
}

function showResult(content) {
    document.getElementById('result').innerHTML = `
        <div class="result-item">
            <div class="result-content">${content}</div>
        </div>
    `;
}

function showLoader(show) {
    const loader = document.getElementById('loader');
    loader.classList.toggle('show', show);
}

function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');
    const isDark = document.body.classList.contains('dark-mode');
    chrome.storage.local.set({ darkMode: isDark });
}
