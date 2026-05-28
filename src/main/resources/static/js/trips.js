(function () {
    const tripList = document.getElementById('tripList');

    function formatDate(dt) {
        if (!dt) return '';
        const d = new Date(dt);
        return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }

    async function load() {
        try {
            const res = await fetch('/api/trip/list?limit=30');
            const json = await res.json();
            if (!json.success || !json.data?.length) {
                tripList.innerHTML = `<div class="empty-result">
                    <p>还没有行程记录</p>
                    <a class="detail-link" href="/">去规划第一条 →</a>
                </div>`;
                return;
            }
            tripList.innerHTML = json.data.map(t => `
                <a class="trip-card" href="/trip/${t.id}">
                    <div class="trip-card-title">${t.title || '行程'}</div>
                    <div class="trip-card-meta">
                        <span>${t.departureCity ? t.departureCity + ' → ' : ''}${t.destinationName || ''}</span>
                        <span>${t.days} 天</span>
                        <span>${t.travelers || ''}</span>
                    </div>
                    <p class="sub-hint">${t.summary || ''}</p>
                    <p class="trip-card-time">${formatDate(t.createdAt)}</p>
                </a>
            `).join('');
        } catch (e) {
            tripList.innerHTML = '<div class="empty-result"><p>加载失败</p></div>';
        }
    }

    load();
})();
