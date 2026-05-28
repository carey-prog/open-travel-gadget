(function () {
    let preset = null;
    let destinationId = 'chaoshan';
    let departureCityId = 'guangzhou';
    let days = 3;
    let travelers = '情侣';
    let budgetTier = '舒适';
    let theme = '美食';
    let transportPreference = 'high_speed_rail';
    let departureDate = '';

    let currentTripId = null;
    let currentSessionId = null;

    const generateBtn = document.getElementById('generateBtn');
    const chatSection = document.getElementById('chatSection');
    const chatLog = document.getElementById('chatLog');
    const refineInput = document.getElementById('refineInput');
    const refineBtn = document.getElementById('refineBtn');
    const resultArea = document.getElementById('resultArea');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const customRequire = document.getElementById('customRequire');
    const configSummary = document.getElementById('configSummary');
    const keyBadges = document.getElementById('keyBadges');
    const departureSelect = document.getElementById('departureSelect');
    const destinationSelect = document.getElementById('destinationSelect');
    const destinationHint = document.getElementById('destinationHint');
    const themeChips = document.getElementById('themeChips');

    const STATUS_TEXT = {
        running: '正在启动 Agent…',
        rag_done: '攻略库检索完成，正在联网搜索交通与景点…',
        web_search_done: '联网信息已获取，正在生成行程…',
        itinerary_generated: '行程已生成，正在保存…'
    };

    async function init() {
        await Promise.all([loadPresets(), loadKeyStatus()]);
        bindChips();
        bindCitySelectors();
        updateSummary();
    }

    async function loadPresets() {
        const res = await fetch('/api/trip/presets');
        const json = await res.json();
        if (!json.success) return;
        preset = json.data;
        destinationId = preset.defaultDestinationId || destinationId;
        departureCityId = preset.defaultDepartureCityId || departureCityId;
        transportPreference = preset.defaultTransportPreference || transportPreference;
        document.getElementById('departureDate').value = defaultDepartureDate();
        departureDate = document.getElementById('departureDate').value;

        departureSelect.innerHTML = (preset.departureCities || []).map(c =>
            `<option value="${c.id}"${c.id === departureCityId ? ' selected' : ''}>${c.name}</option>`
        ).join('');

        destinationSelect.innerHTML = (preset.destinations || []).map(d =>
            `<option value="${d.id}"${d.id === destinationId ? ' selected' : ''}>${d.name}</option>`
        ).join('');

        renderChips('transportChips', (preset.transportPreferences || []).map(t => t.label),
            labelOfTransport(preset, transportPreference),
            label => {
                const found = (preset.transportPreferences || []).find(t => t.label === label);
                if (found) transportPreference = found.id;
                updateSummary();
            });
        renderChips('travelerChips', preset.travelerOptions, travelers, v => { travelers = v; updateSummary(); });
        renderChips('budgetChips', preset.budgetTiers, budgetTier, v => { budgetTier = v; updateSummary(); });
        refreshThemeChips();
    }

    function defaultDepartureDate() {
        const d = new Date();
        d.setDate(d.getDate() + 7);
        return d.toISOString().slice(0, 10);
    }

    function labelOfTransport(preset, id) {
        const t = (preset.transportPreferences || []).find(x => x.id === id);
        return t ? t.label : '优先高铁';
    }

    function bindCitySelectors() {
        document.getElementById('departureDate').addEventListener('change', e => {
            departureDate = e.target.value;
            updateSummary();
        });
        departureSelect.addEventListener('change', () => {
            departureCityId = departureSelect.value;
            updateSummary();
        });
        destinationSelect.addEventListener('change', () => {
            destinationId = destinationSelect.value;
            refreshThemeChips();
            updateSummary();
        });
    }

    function getSelectedDestination() {
        return (preset?.destinations || []).find(d => d.id === destinationId);
    }

    function getSelectedDeparture() {
        return (preset?.departureCities || []).find(c => c.id === departureCityId);
    }

    function refreshThemeChips() {
        const dest = getSelectedDestination();
        const themes = dest?.themeOptions?.length ? dest.themeOptions : ['综合'];
        if (!themes.includes(theme)) {
            theme = themes[0];
        }
        renderChips('themeChips', themes, theme, v => { theme = v; updateSummary(); });
        if (dest) {
            destinationHint.textContent = dest.localTransportNote || '';
        }
    }

    async function loadKeyStatus() {
        try {
            const res = await fetch('/api/settings/keys');
            const json = await res.json();
            if (!json.success) return;
            const s = json.data;
            const items = [];
            items.push(badge(s.deepseekConfigured ? 'DeepSeek ✓' : 'DeepSeek 未配置', !s.deepseekConfigured));
            items.push(badge(s.zhipuConfigured ? '智谱联网 ✓' : '智谱未配置', !s.zhipuConfigured));
            items.push(badge(s.ragKnowledgeLoaded ? 'RAG 已加载' : 'RAG 未就绪', !s.ragKnowledgeLoaded));
            keyBadges.innerHTML = items.join('');
            if (!s.deepseekConfigured) {
                keyBadges.innerHTML += `<a href="/settings" class="badge badge-warn" style="text-decoration:none">去配置 API →</a>`;
            }
        } catch (e) {
            keyBadges.innerHTML = '<span class="badge badge-err">无法检测 API 状态</span>';
        }
    }

    function badge(text, warn) {
        return `<span class="badge ${warn ? 'badge-warn' : 'badge-ok'}">${text}</span>`;
    }

    function renderChips(containerId, options, selected, onSelect) {
        const el = document.getElementById(containerId);
        el.innerHTML = options.map(opt =>
            `<button type="button" class="chip-btn${opt === selected ? ' active' : ''}" data-value="${opt}">${opt}</button>`
        ).join('');
        el.querySelectorAll('.chip-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                el.querySelectorAll('.chip-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                onSelect(btn.dataset.value);
            });
        });
    }

    function bindChips() {
        document.querySelectorAll('#daysChips .chip-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('#daysChips .chip-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                days = Number(btn.dataset.days);
                updateSummary();
            });
        });
    }

    function updateSummary() {
        if (!preset) return;
        const dest = getSelectedDestination();
        const dep = getSelectedDeparture();
        const transportLabel = labelOfTransport(preset, transportPreference);
        if (!dest || !dep) return;
        configSummary.innerHTML = `
            <div>🧳 ${dep.name} → ${dest.name}</div>
            <div>📅 出发：${departureDate || '未选'} · ${days} 天</div>
            <div>🚄 交通偏好：${transportLabel}</div>
            <div>🚉 抵达：${dest.arrivalHubLabel}</div>
            <div>📍 游玩：${(dest.cities || []).join('、')}</div>
            <div>👥 ${travelers} · ${budgetTier} · ${theme}</div>
            <div class="sub-hint">${dest.localTransportNote || ''}</div>`;
    }

    generateBtn.addEventListener('click', async () => {
        const dest = getSelectedDestination();
        showLoading(`正在规划 ${dest?.name || '目的地'} 行程…`);
        generateBtn.disabled = true;
        try {
            const res = await fetch('/api/trip/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    destinationId,
                    departureCityId,
                    departureDate: departureDate || null,
                    transportPreference,
                    days,
                    travelers,
                    budgetTier,
                    theme,
                    customRequire: customRequire.value
                })
            });
            const json = await res.json();
            if (!json.success || !json.data?.sessionId) {
                alert(json.message || '启动失败，请检查 API 配置');
                return;
            }
            currentSessionId = json.data.sessionId;
            const trip = await pollTripResult(currentSessionId);
            if (trip) {
                currentTripId = trip.id;
                renderResult(trip);
                showChatSection();
            }
        } catch (e) {
            alert(e.message || '生成失败');
        } finally {
            hideLoading();
            generateBtn.disabled = false;
        }
    });

    async function pollTripResult(sessionId) {
        const maxWait = 10 * 60 * 1000;
        const interval = 2000;
        const start = Date.now();
        while (Date.now() - start < maxWait) {
            const res = await fetch('/api/agent/session/' + sessionId);
            const json = await res.json();
            const state = json.data || {};
            if (state.status && STATUS_TEXT[state.status]) {
                document.getElementById('loadingText').textContent = STATUS_TEXT[state.status];
            }
            if (state.status === 'completed') {
                const tripId = state.tripId;
                const tripRes = await fetch('/api/trip/' + tripId);
                const tripJson = await tripRes.json();
                if (tripJson.success && tripJson.data) return tripJson.data;
                throw new Error(tripJson.message || '获取行程失败');
            }
            if (state.status === 'failed') {
                throw new Error(state.error || '行程生成失败');
            }
            await sleep(interval);
        }
        throw new Error('生成超时，请查看后端日志');
    }

    function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

    function renderTransportSuggestions(suggestions) {
        if (!suggestions || !suggestions.length) return '';
        const rows = suggestions.map(t => `
            <div class="transport-item ${t.priority === '推荐' ? 'recommended' : ''}">
                <div class="transport-head">
                    <strong>${t.mode || ''}</strong>
                    <span class="transport-priority">${t.priority || ''}</span>
                </div>
                <div>${t.route || ''} · ${t.duration || ''}</div>
                <div class="sub-hint">${t.scheduleHint || ''} ${t.priceHint ? '· ' + t.priceHint : ''}</div>
                ${t.note ? `<div class="slot-tips">💡 ${t.note}</div>` : ''}
            </div>`).join('');
        return `<div class="section-block transport-block">
            <h3>🚄✈️ 大交通建议（出发地 → 目的地）</h3>
            ${rows}
        </div>`;
    }

    function renderResult(trip) {
        const it = trip.itinerary || {};
        const transportHtml = renderTransportSuggestions(it.transportSuggestions);

        const daysHtml = (it.days || []).map(d => {
            const slots = (d.slots || []).map(s =>
                `<div class="slot-item">
                    <strong>${s.period || ''} · ${s.poiName || ''}</strong>
                    <div>${s.activity || ''}（约 ${s.durationMinutes || '-'} 分钟）</div>
                    ${s.transport ? `<div class="slot-tips">🚗 ${s.transport}</div>` : ''}
                    ${s.tips ? `<div class="slot-tips">💡 ${s.tips}</div>` : ''}
                </div>`
            ).join('');
            return `<div class="day-block">
                <h3>第 ${d.dayIndex} 天 · ${d.city || ''} ${d.theme ? '· ' + d.theme : ''}</h3>
                ${d.accommodationArea ? `<p class="sub-hint">住宿建议：${d.accommodationArea}</p>` : ''}
                ${slots}
            </div>`;
        }).join('');

        const foodHtml = (it.foodRecommendations || []).map(f =>
            `<span class="food-tag">${f}</span>`
        ).join('');

        const warnHtml = (it.warnings || []).length
            ? `<div class="warning-list"><strong>⚠️ 注意事项</strong><ul>${(it.warnings || []).map(w => `<li>${w}</li>`).join('')}</ul></div>` : '';

        const budget = trip.budget || it.budget || {};
        const budgetHtml = budget.totalMin != null
            ? `<div class="budget-box">💰 预算估算（${budget.tier || trip.budgetTier}）：人均每日约 ¥${budget.perPersonPerDayMin}–${budget.perPersonPerDayMax}，全程约 ¥${budget.totalMin}–${budget.totalMax}。${budget.note || ''}</div>` : '';

        const dep = trip.departureCity || it.departureCity || '';
        const dest = trip.destinationName || it.destinationName || '';
        const actionBar = currentTripId ? `
            <div class="result-actions">
                <button type="button" class="secondary-btn" onclick="window.exportTrip(${currentTripId})">导出 Markdown</button>
                <button type="button" class="secondary-btn" onclick="window.shareTrip(${currentTripId})">分享链接</button>
            </div>` : '';

        resultArea.innerHTML = `
            <div class="trip-result">
                <h2>${it.title || trip.title || '行程方案'}</h2>
                ${actionBar}
                <div class="meta-tags">
                    <span class="meta-tag">${dep} → ${dest}</span>
                    ${trip.departureDate ? `<span class="meta-tag">📅 ${trip.departureDate}</span>` : ''}
                    ${trip.transportPreferenceLabel ? `<span class="meta-tag">${trip.transportPreferenceLabel}</span>` : ''}
                    <span class="meta-tag">${trip.days} 天</span>
                    <span class="meta-tag">${trip.travelers}</span>
                    <span class="meta-tag">${trip.budgetTier}</span>
                    <span class="meta-tag">🚉 ${trip.arrivalHubLabel || ''}</span>
                </div>
                <p class="hint">${it.summary || trip.summary || ''}</p>
                <p class="sub-hint">${it.transportNote || ''}</p>
                ${transportHtml}
                ${budgetHtml}
                ${daysHtml}
                ${foodHtml ? `<div class="section-block"><h3 style="margin-top:16px">🍲 美食推荐</h3><div class="food-tags">${foodHtml}</div></div>` : ''}
                ${warnHtml}
                <a class="detail-link" href="/trip/${trip.id}">查看完整详情 →</a>
            </div>`;
        resultArea.scrollIntoView({ behavior: 'smooth' });
    }

    function showChatSection() {
        chatSection.hidden = false;
        chatLog.innerHTML = '<div class="chat-msg assistant"><span>行程已生成。你可以继续描述想怎么改，我会基于当前方案调整。</span></div>';
    }

    function appendChat(role, text) {
        const cls = role === 'user' ? 'user' : 'assistant';
        chatLog.insertAdjacentHTML('beforeend',
            `<div class="chat-msg ${cls}"><span>${escapeHtml(text)}</span></div>`);
        chatLog.scrollTop = chatLog.scrollHeight;
    }

    function escapeHtml(s) {
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    async function refineTrip(message) {
        if (!currentTripId) {
            alert('请先生成行程');
            return;
        }
        appendChat('user', message);
        refineBtn.disabled = true;
        showLoading('正在根据你的要求调整行程…');
        try {
            const res = await fetch('/api/trip/refine', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tripId: currentTripId, message })
            });
            const json = await res.json();
            if (!json.success || !json.data) {
                throw new Error(json.message || '调整失败');
            }
            renderResult(json.data);
            appendChat('assistant', json.data.summary || '行程已更新，请查看上方结果。');
            refineInput.value = '';
        } catch (e) {
            appendChat('assistant', '调整失败：' + (e.message || '未知错误'));
        } finally {
            hideLoading();
            refineBtn.disabled = false;
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
    document.querySelectorAll('.quick-refine').forEach(btn => {
        btn.addEventListener('click', () => refineTrip(btn.dataset.msg));
    });

    function showLoading(text) {
        document.getElementById('loadingText').textContent = text;
        loadingOverlay.hidden = false;
    }
    function hideLoading() { loadingOverlay.hidden = true; }

    window.exportTrip = function (id) {
        window.location.href = '/api/trip/' + id + '/export';
    };

    window.shareTrip = async function (id) {
        try {
            const res = await fetch('/api/trip/' + id + '/share', { method: 'POST' });
            const json = await res.json();
            if (!json.success) {
                alert(json.message || '生成失败');
                return;
            }
            const path = json.data?.sharePath || '';
            const url = window.location.origin + path;
            await navigator.clipboard.writeText(url);
            alert('分享链接已复制：\n' + url);
        } catch (e) {
            alert('分享失败');
        }
    };

    init();
})();
