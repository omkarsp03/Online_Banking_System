const API_BASE = '';
let authToken = localStorage.getItem('authToken');
let currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
let accounts = [];

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initForms();
    updateGreeting();
    if (authToken && currentUser) {
        showPage('dashboard-page');
        loadDashboard();
    } else {
        showPage('login-page');
    }
});

// Page Navigation
function showPage(pageId) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');
}

function showSection(sectionId) {
    document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.sidebar-item').forEach(i => i.classList.remove('active'));
    document.getElementById(sectionId).classList.add('active');
    document.querySelector(`.sidebar-item[data-section="${sectionId}"]`).classList.add('active');
    if (sectionId === 'beneficiaries') {
        loadBeneficiaries();
    }
}

// Form Initialization
function initForms() {
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('register-form').addEventListener('submit', handleRegister);
    document.getElementById('new-account-form').addEventListener('submit', handleCreateAccount);
    document.getElementById('transfer-form').addEventListener('submit', handleTransfer);
    document.getElementById('deposit-form').addEventListener('submit', handleDepositSubmit);
    document.getElementById('withdraw-form').addEventListener('submit', handleWithdrawSubmit);
    document.getElementById('new-beneficiary-form').addEventListener('submit', handleCreateBeneficiary);
}

// API Helper
async function apiRequest(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }
    
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || 'Request failed');
        }
        
        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Authentication
async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    
    try {
        const result = await apiRequest('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        
        authToken = result.data.token;
        currentUser = {
            username: result.data.username,
            role: result.data.role
        };
        
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        
        showNotification('Login successful!', 'success');
        showPage('dashboard-page');
        loadDashboard();
    } catch (error) {
        showNotification(error.message || 'Login failed', 'error');
    }
}

async function handleRegister(e) {
    e.preventDefault();
    
    const data = {
        email: document.getElementById('register-email').value,
        password: document.getElementById('register-password').value,
        firstName: document.getElementById('register-firstName').value,
        lastName: document.getElementById('register-lastName').value,
        phoneNumber: document.getElementById('register-phone').value
    };
    
    try {
        const result = await apiRequest('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        authToken = result.data.token;
        currentUser = {
            username: result.data.username,
            role: result.data.role
        };
        
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        
        showNotification('Account created successfully!', 'success');
        showPage('dashboard-page');
        loadDashboard();
    } catch (error) {
        showNotification(error.message || 'Registration failed', 'error');
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    accounts = [];
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    showPage('login-page');
    showNotification('Logged out successfully', 'info');
}

// Dashboard
async function loadDashboard() {
    updateGreeting();
    await loadAccounts();
    await loadTransactions();
}

function updateGreeting() {
    const hour = new Date().getHours();
    let greeting = 'day';
    if (hour < 12) greeting = 'morning';
    else if (hour < 18) greeting = 'afternoon';
    else greeting = 'evening';
    
    document.getElementById('greeting-time').textContent = greeting;
    if (currentUser) {
        const name = currentUser.username.split('@')[0];
        document.getElementById('user-name').textContent = capitalizeFirst(name);
        document.getElementById('user-greeting').textContent = `Welcome, ${capitalizeFirst(name)}`;
    }
}

function capitalizeFirst(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

// Accounts
async function loadAccounts() {
    try {
        const result = await apiRequest('/api/customer/accounts');
        accounts = result.data || [];
        renderAccounts();
        updateAccountSelects();
        updateStats();
    } catch (error) {
        console.error('Failed to load accounts:', error);
    }
}

function renderAccounts() {
    const container = document.getElementById('accounts-list');
    
    if (accounts.length === 0) {
        container.innerHTML = '<p class="empty-state">No accounts yet. Create your first account!</p>';
        return;
    }
    
    container.innerHTML = accounts.map(account => `
        <div class="account-card">
            <div class="account-card-content">
                <p class="account-type">${account.accountType}</p>
                <p class="account-number">•••• ${account.accountNumber.slice(-4)}</p>
                <p class="account-balance">${formatCurrency(account.balance, account.currency)}</p>
                <p class="account-currency">${account.currency}</p>
                <div class="account-actions">
                    <button class="btn" onclick="quickDeposit('${account.accountNumber}')">Deposit</button>
                    <button class="btn" onclick="quickWithdraw('${account.accountNumber}')">Withdraw</button>
                </div>
            </div>
        </div>
    `).join('');
}

function updateAccountSelects() {
    const select = document.getElementById('transfer-from');
    select.innerHTML = '<option value="">Select account</option>' + 
        accounts.map(a => `<option value="${a.accountNumber}">${a.accountType} (${a.accountNumber.slice(-4)}) - ${formatCurrency(a.balance, a.currency)}</option>`).join('');

    const exportSelect = document.getElementById('export-account');
    if (exportSelect) {
        exportSelect.innerHTML = '<option value="">Select Account</option>' + 
            accounts.map(a => `<option value="${a.accountNumber}">${a.accountType} (${a.accountNumber.slice(-4)})</option>`).join('');
    }
}

function updateStats() {
    const totalBalance = accounts.reduce((sum, a) => sum + parseFloat(a.balance), 0);
    document.getElementById('total-balance').textContent = formatCurrency(totalBalance, 'USD');
}

// Modal
function openNewAccountModal() {
    document.getElementById('account-modal').classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

async function handleCreateAccount(e) {
    e.preventDefault();
    
    const data = {
        accountType: document.getElementById('account-type').value,
        currency: document.getElementById('account-currency').value
    };
    
    try {
        await apiRequest('/api/customer/accounts', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        showNotification('Account created successfully!', 'success');
        closeModal('account-modal');
        document.getElementById('new-account-form').reset();
        await loadAccounts();
    } catch (error) {
        showNotification(error.message || 'Failed to create account', 'error');
    }
}

// Transactions
let currentTransactionsPage = 0;
const transactionsPageSize = 5;

async function loadTransactions() {
    try {
        const result = await apiRequest(`/api/customer/transactions?page=${currentTransactionsPage}&size=${transactionsPageSize}&sort=createdAt,desc`);
        
        // Fetch last 50 transactions to calculate overall stats
        const statsResult = await apiRequest('/api/customer/transactions?page=0&size=50&sort=createdAt,desc');
        const statsTx = (statsResult.data && statsResult.data.content) || [];
        updateStatsFromTransactions(statsTx);
        
        const pagedData = result.data || { content: [], totalPages: 1 };
        const transactions = pagedData.content || [];
        renderTransactions(transactions);
        updatePaginationControls(pagedData);
    } catch (error) {
        console.error('Failed to load transactions:', error);
    }
}

function updateStatsFromTransactions(transactions) {
    let income = 0;
    let expenses = 0;
    
    transactions.forEach(t => {
        const amount = parseFloat(t.amount);
        if (t.transactionType === 'DEPOSIT' || t.transactionType === 'INTEREST') {
            income += amount;
        } else if (t.transactionType === 'WITHDRAWAL') {
            expenses += amount;
        } else if (t.transactionType === 'TRANSFER') {
            const isIncoming = accounts.some(a => a.accountNumber === t.destinationAccountNumber);
            if (isIncoming) {
                income += amount;
            } else {
                expenses += amount;
            }
        }
    });
    
    document.getElementById('total-income').textContent = formatCurrency(income, 'USD');
    document.getElementById('total-expenses').textContent = formatCurrency(expenses, 'USD');
}

function updatePaginationControls(pagedData) {
    const prevBtn = document.getElementById('transactions-prev-btn');
    const nextBtn = document.getElementById('transactions-next-btn');
    const pageNum = document.getElementById('transactions-page-num');
    
    if (!prevBtn || !nextBtn || !pageNum) return;
    
    const page = pagedData.page !== undefined ? pagedData.page : currentTransactionsPage;
    const totalPages = pagedData.totalPages || 1;
    
    prevBtn.disabled = page <= 0;
    nextBtn.disabled = page >= totalPages - 1;
    pageNum.textContent = `Page ${page + 1} of ${totalPages}`;
}

async function changeTransactionsPage(direction) {
    currentTransactionsPage += direction;
    await loadTransactions();
}

function renderTransactions(transactions) {
    const recentContainer = document.getElementById('recent-transactions');
    const allContainer = document.getElementById('transactions-list');
    
    if (transactions.length === 0) {
        const empty = '<p class="empty-state">No transactions yet</p>';
        if (recentContainer) recentContainer.innerHTML = empty;
        if (allContainer) allContainer.innerHTML = empty;
        return;
    }
    
    // Recent activity shows latest 5 items
    const html = transactions.slice(0, 5).map(t => createTransactionHTML(t)).join('');
    if (recentContainer) recentContainer.innerHTML = html;
    if (allContainer) allContainer.innerHTML = transactions.map(t => createTransactionHTML(t)).join('');
}

function createTransactionHTML(transaction) {
    let isPositive = transaction.transactionType === 'DEPOSIT' || transaction.transactionType === 'INTEREST';
    if (transaction.transactionType === 'TRANSFER') {
        isPositive = accounts.some(a => a.accountNumber === transaction.destinationAccountNumber);
    }
    const iconClass = transaction.transactionType.toLowerCase();
    
    return `
        <div class="transaction-item">
            <div class="transaction-info">
                <div class="transaction-icon ${iconClass} ${isPositive ? 'positive' : 'negative'}">
                    ${getTransactionIcon(transaction.transactionType, isPositive)}
                </div>
                <div class="transaction-details">
                    <h4>${transaction.description || transaction.transactionType}</h4>
                    <p>${new Date(transaction.createdAt).toLocaleDateString()}</p>
                </div>
            </div>
            <p class="transaction-amount ${isPositive ? 'positive' : 'negative'}">
                ${isPositive ? '+' : '-'}${formatCurrency(Math.abs(transaction.amount), transaction.currency)}
            </p>
        </div>
    `;
}

function getTransactionIcon(type, isPositive) {
    if (type === 'DEPOSIT' || (type === 'TRANSFER' && isPositive)) {
        return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>';
    } else if (type === 'WITHDRAWAL' || (type === 'TRANSFER' && !isPositive)) {
        return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 6 9.5 15.5 14.5 10.5 22 18"/><polyline points="7 6 1 6 1 12"/></svg>';
    } else {
        return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>';
    }
}

// Transfer
async function handleTransfer(e) {
    e.preventDefault();
    
    const data = {
        sourceAccountNumber: document.getElementById('transfer-from').value,
        destinationAccountNumber: document.getElementById('transfer-to').value,
        amount: parseFloat(document.getElementById('transfer-amount').value),
        currency: document.getElementById('transfer-currency').value,
        description: document.getElementById('transfer-description').value
    };
    
    try {
        await apiRequest('/api/customer/transactions', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        showNotification('Transfer completed successfully!', 'success');
        document.getElementById('transfer-form').reset();
        await loadAccounts();
        await loadTransactions();
        showSection('overview');
    } catch (error) {
        showNotification(error.message || 'Transfer failed', 'error');
    }
}

// Quick Actions
function quickDeposit(accountNumber) {
    document.getElementById('deposit-accountNumber').value = accountNumber;
    document.getElementById('deposit-amount').value = '';
    document.getElementById('deposit-description').value = 'Quick deposit';
    document.getElementById('deposit-modal').classList.add('active');
}

async function handleDepositSubmit(e) {
    e.preventDefault();
    const accountNumber = document.getElementById('deposit-accountNumber').value;
    const amount = parseFloat(document.getElementById('deposit-amount').value);
    const description = document.getElementById('deposit-description').value;

    try {
        await apiRequest(`/api/customer/accounts/${accountNumber}/deposit`, {
            method: 'POST',
            body: JSON.stringify({
                amount: amount,
                currency: 'USD',
                description: description
            })
        });
        
        showNotification('Deposit successful!', 'success');
        closeModal('deposit-modal');
        await loadAccounts();
        await loadTransactions();
    } catch (error) {
        showNotification(error.message || 'Deposit failed', 'error');
    }
}

function quickWithdraw(accountNumber) {
    document.getElementById('withdraw-accountNumber').value = accountNumber;
    document.getElementById('withdraw-amount').value = '';
    document.getElementById('withdraw-description').value = 'Quick withdrawal';
    document.getElementById('withdraw-modal').classList.add('active');
}

async function handleWithdrawSubmit(e) {
    e.preventDefault();
    const accountNumber = document.getElementById('withdraw-accountNumber').value;
    const amount = parseFloat(document.getElementById('withdraw-amount').value);
    const description = document.getElementById('withdraw-description').value;

    try {
        await apiRequest(`/api/customer/accounts/${accountNumber}/withdraw`, {
            method: 'POST',
            body: JSON.stringify({
                amount: amount,
                currency: 'USD',
                description: description
            })
        });
        
        showNotification('Withdrawal successful!', 'success');
        closeModal('withdraw-modal');
        await loadAccounts();
        await loadTransactions();
    } catch (error) {
        showNotification(error.message || 'Withdrawal failed', 'error');
    }
}

// Beneficiaries
let beneficiaries = [];

async function loadBeneficiaries() {
    try {
        const result = await apiRequest('/api/customer/beneficiaries');
        beneficiaries = result.data || [];
        renderBeneficiaries();
    } catch (error) {
        console.error('Failed to load beneficiaries:', error);
    }
}

function renderBeneficiaries() {
    const container = document.getElementById('beneficiaries-list');
    if (!container) return;

    if (beneficiaries.length === 0) {
        container.innerHTML = '<tr><td colspan="5" class="empty-state text-center">No beneficiaries added yet.</td></tr>';
        return;
    }

    container.innerHTML = beneficiaries.map(b => `
        <tr>
            <td><strong>${b.beneficiaryName}</strong></td>
            <td><code>${b.beneficiaryAccountNumber}</code></td>
            <td>${b.beneficiaryBank}</td>
            <td><span class="status-badge ${b.status.toLowerCase()}">${b.status}</span></td>
            <td>
                <div class="table-actions">
                    ${b.status === 'PENDING' ? `<button class="btn btn-secondary btn-small" onclick="verifyBeneficiary(${b.id})">Verify</button>` : ''}
                    <button class="btn btn-danger btn-small" onclick="deleteBeneficiary(${b.id})">Delete</button>
                </div>
            </td>
        </tr>
    `).join('');
}

function openNewBeneficiaryModal() {
    document.getElementById('beneficiary-modal').classList.add('active');
}

async function handleCreateBeneficiary(e) {
    e.preventDefault();
    const data = {
        beneficiaryName: document.getElementById('beneficiary-name').value,
        beneficiaryAccountNumber: document.getElementById('beneficiary-account').value,
        beneficiaryBank: document.getElementById('beneficiary-bank').value
    };

    try {
        await apiRequest('/api/customer/beneficiaries', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        showNotification('Beneficiary added! Pending activation.', 'success');
        closeModal('beneficiary-modal');
        document.getElementById('new-beneficiary-form').reset();
        await loadBeneficiaries();
    } catch (error) {
        showNotification(error.message || 'Failed to add beneficiary', 'error');
    }
}

async function verifyBeneficiary(id) {
    try {
        await apiRequest(`/api/customer/beneficiaries/${id}/verify`, {
            method: 'POST'
        });
        showNotification('Beneficiary verified successfully!', 'success');
        await loadBeneficiaries();
    } catch (error) {
        showNotification(error.message || 'Failed to verify beneficiary', 'error');
    }
}

async function deleteBeneficiary(id) {
    if (!confirm('Are you sure you want to delete this beneficiary?')) return;
    try {
        await apiRequest(`/api/customer/beneficiaries/${id}`, {
            method: 'DELETE'
        });
        showNotification('Beneficiary deleted successfully', 'success');
        await loadBeneficiaries();
    } catch (error) {
        showNotification(error.message || 'Failed to delete beneficiary', 'error');
    }
}

// Statement Export
async function handleExportStatement() {
    const accountNumber = document.getElementById('export-account').value;
    const format = document.getElementById('export-format').value;

    if (!accountNumber) {
        showNotification('Please select an account', 'error');
        return;
    }

    try {
        const response = await fetch(`/api/customer/accounts/${accountNumber}/statement?format=${format}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to export statement');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `statement_${accountNumber}.${format}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
        showNotification('Statement downloaded successfully', 'success');
    } catch (error) {
        showNotification(error.message || 'Export failed', 'error');
    }
}

// Utilities
function formatCurrency(amount, currency = 'USD') {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency
    }).format(amount);
}

function showNotification(message, type = 'info') {
    const container = document.getElementById('notification-container');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    
    const icons = {
        success: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>',
        error: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
        info: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
    };
    
    notification.innerHTML = `${icons[type] || icons.info}<span>${message}</span>`;
    container.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 4000);
}

// Close modal on outside click
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('active');
    }
});
