(function () {
    const tripId = window.TRIP_ID;
    const container = document.getElementById('tripDetail');
    const chatSection = document.getElementById('detailChatSection');
    const chatLog = document.getElementById('detailChatLog');
    const refineInput = document.getElementById('detailRefineInput');
    const refineBtn = document.getElementById('detailRefineBtn');
    const detailActions = document.getElementById('detailActions');
    const exportBtn = document.getElementById('exportBtn');
    const shareBtn = document.getElementById('shareBtn');

    let currentTrip = null;

    function renderTransport(suggestions) {
        if (!suggestions || !suggestions.length) return '';
        const rows = suggestions.map(t => `
            <div class="transport-item ${t.priority === '推荐' ? 'recommended' : ''}">
                <div class="transport-head"><strong>${t.mode || ''}</strong><span class="transport-priority">${t.priority || ''}</span></div>
                <div>${t.route || ''} · ${t.duration || ''}</div>
                <div class="sub-hint">${t.scheduleHint || ''} ${t.priceHint ? '· ' + t.priceHint : ''}</div>
            </div>`).join('');
        return `<h3 style="margin-top:16px">🚄✈️ 大交通建议</h3>${rows}`;
    }

    async function load() {
        if (!tripId) {
            container.innerHTML = '<div class="card-body"><p>无效的行程 ID</p></div>';
            return;
        }
        try {
            const res = await fetch('/api/trip/' + tripId);
            const json = await res.json();
            if (!json.success || !json.data) {
                container.innerHTML = `<div class="card-body"><p>${json.message || '行程不存在'}</p></div>`;
                return;
            }
            currentTrip = json.data;
            render(currentTrip);
            detailActions.hidden = false;
            chatSection.hidden = false;
            chatLog.innerHTML = '<div class="chat-msg assistant"><span>可在下方继续调整本行程。</span></div>';
        } catch (e) {
            container.innerHTML = '<div class="card-body"><p>加载失败</p></div>';
        }
    }

    function render(trip) {
        const it = trip.itinerary || {};
        const dep = trip.departureCity || it.departureCity || '';
        const dest = trip.destinationName || it.destinationName || '';
        const daysHtml = (it.days || []).map(d => {
            const slots = (d.slots || []).map(s =>
                `<div class="slot-item">
                    <strong>${s.period || ''} · ${s.poiName || ''}</strong>
                    <div>${s.activity || ''}</div>
                    <div class="slot-tips">约 ${s.durationMinutes || '-'} 分钟 · ${s.transport || ''}</div>
                    ${s.tips ? `<div class="slot-tips">💡 ${s.tips}</div>` : ''}
                </div>`
            ).join('');
            return `<div class="day-block">
                <h3>第 ${d.dayIndex} 天 · ${d.city || ''}</h3>
                <p class="hint">${d.theme || ''}</p>
                ${slots}
            </div>`;
        }).join('');

        const budget = trip.budget || it.budget || {};
        container.innerHTML = `<div class="card-body trip-result">
            <h2>${it.title || trip.title}</h2>
            <div class="meta-tags">
                <span class="meta-tag">${dep} → ${dest}</span>
                <span class="meta-tag">${trip.days} 天</span>
                <span class="meta-tag">${trip.travelers}</span>
                <span class="meta-tag">${trip.budgetTier}</span>
            </div>
            <p class="hint">${it.summary || trip.summary}</p>
            <p class="sub-hint">🚉 ${trip.arrivalHubLabel || ''} · ${it.transportNote || ''}</p>
            ${renderTransport(it.transportSuggestions)}
            ${budget.totalMin != null ? `<div class="budget-box">全程约 ¥${budget.totalMin}–${budget.totalMax}</div>` : ''}
            ${daysHtml}
            ${(it.foodRecommendations || []).length ? `<h3 style="margin-top:20px">🍲 美食</h3><div class="food-tags">${it.foodRecommendations.map(f => `<span class="food-tag">${f}</span>`).join('')}</div>` : ''}
            ${(it.warnings || []).length ? `<div class="warning-list"><ul>${it.warnings.map(w => `<li>${w}</li>`).join('')}</ul></div>` : ''}
        </div>`;
    }

    async function refineTrip(message) {
        refineBtn.disabled = true;
        chatLog.insertAdjacentHTML('beforeend', `<div class="chat-msg user"><span>${message}</span></div>`);
        try {
            const res = await fetch('/api/trip/refine', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tripId: Number(tripId), message })
            });
            const json = await res.json();
            if (!json.success) throw new Error(json.message);
            currentTrip = json.data;
            render(currentTrip);
            chatLog.insertAdjacentHTML('beforeend',
                `<div class="chat-msg assistant"><span>${json.data.summary || '已更新'}</span></div>`);
            refineInput.value = '';
        } catch (e) {
            chatLog.insertAdjacentHTML('beforeend',
                `<div class="chat-msg assistant"><span>失败：${e.message}</span></div>`);
        } finally {
            refineBtn.disabled = false;
            chatLog.scrollTop = chatLog.scrollHeight;
        }
    }

    refineBtn.addEventListener('click', () => {
        const msg = refineInput.value.trim();
        if (msg) refineTrip(msg);
    });
    refineInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const msg = refineInput.value.trim();
            if (msg) refineTrip(msg);
        }
    });

    exportBtn.addEventListener('click', () => {
        if (tripId) window.location.href = '/api/trip/' + tripId + '/export';
    });
    shareBtn.addEventListener('click', async () => {
        try {
            const res = await fetch('/api/trip/' + tripId + '/share', { method: 'POST' });
            const json = await res.json();
            if (!json.success) throw new Error(json.message);
            const url = window.location.origin + (json.data?.sharePath || '');
            await navigator.clipboard.writeText(url);
            alert('分享链接已复制：\n' + url);
        } catch (e) {
            alert('分享失败：' + (e.message || ''));
        }
    });

    load();
})();
