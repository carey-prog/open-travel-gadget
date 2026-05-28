(function () {
    const form = document.getElementById('keysForm');
    const statusGrid = document.getElementById('statusGrid');
    const settingsNote = document.getElementById('settingsNote');
    const ragStatusHint = document.getElementById('ragStatusHint');
    const ragStatusDetail = document.getElementById('ragStatusDetail');
    const ragRebuildBtn = document.getElementById('ragRebuildBtn');

    async function loadStatus() {
        const res = await fetch('/api/settings/keys');
        const json = await res.json();
        if (!json.success) {
            settingsNote.textContent = json.message || '加载失败';
            return;
        }
        const s = json.data;
        settingsNote.textContent = s.note || '';
        document.getElementById('deepseekMasked').textContent = '当前：' + s.deepseekMasked;
        document.getElementById('dashscopeMasked').textContent = '当前：' + s.dashscopeMasked;
        document.getElementById('zhipuMasked').textContent = '当前：' + s.zhipuMasked;

        statusGrid.innerHTML = `
            <div class="status-item ${s.deepseekConfigured ? 'ok' : 'off'}">
                <div class="label">DeepSeek</div>
                <div>${s.deepseekConfigured ? '已配置' : '未配置'}</div>
            </div>
            <div class="status-item ${s.dashscopeConfigured ? 'ok' : 'off'}">
                <div class="label">DashScope</div>
                <div>${s.dashscopeConfigured ? '已配置' : '未配置'}</div>
            </div>
            <div class="status-item ${s.zhipuConfigured ? 'ok' : 'off'}">
                <div class="label">智谱联网</div>
                <div>${s.zhipuConfigured ? '已配置' : '未配置'}</div>
            </div>
            <div class="status-item ${s.ragKnowledgeLoaded ? 'ok' : 'off'}">
                <div class="label">RAG 知识库</div>
                <div>${s.ragKnowledgeLoaded ? '已加载' : '未加载'}</div>
            </div>`;
    }

    async function loadRagStatus() {
        try {
            const res = await fetch('/api/system/rag/status');
            const json = await res.json();
            if (!json.success || !json.data) {
                ragStatusHint.textContent = '无法获取 RAG 状态';
                return;
            }
            const r = json.data;
            ragStatusHint.textContent = r.ready
                ? `已就绪：${r.loadedFileCount} 个文件，${r.totalChunks} 个向量片段`
                : 'RAG 未就绪，请配置 DashScope 与 Redis Stack';
            ragStatusDetail.textContent = JSON.stringify(r, null, 2);
        } catch (e) {
            ragStatusHint.textContent = '加载 RAG 状态失败';
        }
    }

    ragRebuildBtn.addEventListener('click', async () => {
        if (!confirm('将清空并重建 Redis 向量索引，继续？')) return;
        ragRebuildBtn.disabled = true;
        try {
            const res = await fetch('/api/system/rag/rebuild', { method: 'POST' });
            const json = await res.json();
            if (!json.success) {
                alert(json.message || '重建失败');
                return;
            }
            alert(json.message || '重建完成');
            await Promise.all([loadStatus(), loadRagStatus()]);
        } catch (e) {
            alert('请求失败');
        } finally {
            ragRebuildBtn.disabled = false;
        }
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.disabled = true;
        try {
            const body = {};
            const ds = document.getElementById('deepseekKey').value.trim();
            const dw = document.getElementById('dashscopeKey').value.trim();
            const zp = document.getElementById('zhipuKey').value.trim();
            if (ds) body.deepseekApiKey = ds;
            if (dw) body.dashscopeApiKey = dw;
            if (zp) body.zhipuApiKey = zp;

            const res = await fetch('/api/settings/keys', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const json = await res.json();
            if (!json.success) {
                alert(json.message || '保存失败');
                return;
            }
            alert(json.message || '保存成功');
            document.getElementById('deepseekKey').value = '';
            document.getElementById('dashscopeKey').value = '';
            document.getElementById('zhipuKey').value = '';
            await loadStatus();
        } catch (err) {
            alert('请求失败');
        } finally {
            saveBtn.disabled = false;
        }
    });

    loadStatus();
    loadRagStatus();
})();
