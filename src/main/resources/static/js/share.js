(function () {
    const token = window.SHARE_TOKEN;
    const container = document.getElementById('shareContent');

    function renderTransport(suggestions) {
        if (!suggestions || !suggestions.length) return '';
        return '<h3>大交通建议</h3>' + suggestions.map(t =>
            `<div class="transport-item"><strong>${t.mode}</strong>（${t.priority || ''}）<br>
            ${t.route || ''} · ${t.duration || ''}<br>
            <span class="sub-hint">${t.scheduleHint || ''} ${t.priceHint || ''}</span></div>`
        ).join('');
    }

    async function load() {
        if (!token) {
            container.innerHTML = '<div class="card-body"><p>无效链接</p></div>';
            return;
        }
        try {
            const res = await fetch('/api/share/' + token);
            const json = await res.json();
            if (!json.success || !json.data) {
                container.innerHTML = `<div class="card-body"><p>${json.message || '无法加载'}</p></div>`;
                return;
            }
            const trip = json.data;
            const it = trip.itinerary || {};
            const dep = trip.departureCity || it.departureCity || '';
            const dest = trip.destinationName || it.destinationName || '';
            const daysHtml = (it.days || []).map(d =>
                `<div class="day-block"><h3>第 ${d.dayIndex} 天 · ${d.city || ''}</h3>
                ${(d.slots || []).map(s => `<div class="slot-item">${s.period} ${s.poiName}: ${s.activity}</div>`).join('')}
                </div>`
            ).join('');
            container.innerHTML = `<div class="card-body trip-result">
                <h2>${it.title || trip.title}</h2>
                <p class="hint">${dep} → ${dest} · ${trip.days} 天</p>
                ${trip.departureDate ? `<p class="sub-hint">出发日：${trip.departureDate}</p>` : ''}
                <p>${it.summary || ''}</p>
                ${renderTransport(it.transportSuggestions)}
                ${daysHtml}
            </div>`;
        } catch (e) {
            container.innerHTML = '<div class="card-body"><p>加载失败</p></div>';
        }
    }

    load();
})();
