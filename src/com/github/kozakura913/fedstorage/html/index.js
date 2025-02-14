let previousItemData = {item: [], fluid: [], energy: []};
let previousEnergyTimestamp=null;
// ロケールによって表示するテキストを変更する
window.addEventListener("load", function () {
    document.getElementById('item-info-title').innerText = localeText[locale].itemInfoTitle;
    document.getElementById('channel-header').innerText = localeText[locale].channelHeader;
    document.getElementById('queue-header').innerText = localeText[locale].queueHeader;
    document.getElementById('fluid-info-title').innerText = localeText[locale].fluidInfoTitle;
    document.getElementById('fluid-channel-header').innerText = localeText[locale].fluidChannelHeader;
    document.getElementById('fluid-type-header').innerText = localeText[locale].fluidTypeHeader;
    document.getElementById('energy-info-title').innerText = localeText[locale].energyInfoTitle;
    document.getElementById('energy-channel-header').innerText = localeText[locale].energyChannelHeader;
    document.getElementById('energy-type-header').innerText = localeText[locale].energyAmountHeader;
    document.getElementById('host-info-title').innerText = localeText[locale].clientHostName;
});
async function fetchItem() {
    const response = await fetch('/api/list/item_frequency.json');
    const data = await response.json();
    const table = document.getElementById('item-list').getElementsByTagName('tbody')[0];

    // 行数を調整
    while (table.rows.length < data.length) {
        table.insertRow();
    }
    while (table.rows.length > data.length) {
        table.deleteRow(-1);
    }

    // 行を上から書き換え
    data.forEach((item, index) => {
        const row = table.rows[index];
        let cell1 = row.cells[0];
        let cell2 = row.cells[1];
        if (!cell1) cell1 = row.insertCell(0);
        if (!cell2) cell2 = row.insertCell(1);

        const ids = item.id.split(',').map(id => `<div class="freq ${id}"></div>`).join('');
        const localeText = item.id.split(',').map(id => localeColour[locale][id] || id).join(', ');
        cell1.innerHTML = `<a href="/items.html?freq=${item.id.toUpperCase()}&lang=${locale}">`+ids + '</a> ' + `<span class="txt freq-guide">${localeText}</span>`;

        const previousItem = previousItemData.item[index] || {};
        const difference = item.size - (previousItem.size || 0);
        const differenceText = difference > 0 ? `+${difference}` : difference;
        cell2.innerHTML = `${item.size.toLocaleString()} <span class="diff-value ${difference > 0 ? 'add' : difference < 0 ? 'sub' : 'zero'}">${difference==0?"±":""}${differenceText.toLocaleString()}</span>`;
        cell2.classList.add('right-align');
    });

    // 現在のデータを保存
    previousItemData.item = data;
}

async function fetchFluid() {
    const response = await fetch('/api/list/fluid_frequency.json');
    const data = await response.json();
    const table = document.getElementById('fluid-list').getElementsByTagName('tbody')[0];

    // 行数を調整
    while (table.rows.length < data.length) {
        table.insertRow();
    }
    while (table.rows.length > data.length) {
        table.deleteRow(-1);
    }

    // 行を上から書き換え
    data.forEach((item, index) => {
        const row = table.rows[index];
        let cell1 = row.cells[0];
        let cell2 = row.cells[1];
        if (!cell1) cell1 = row.insertCell(0);
        if (!cell2) cell2 = row.insertCell(1);

        const ids = item.id.split(',').map(id => `<div class="freq ${id}"></div>`).join('');
        const localeText = item.id.split(',').map(id => localeColour[locale][id] || id).join(', ');
        cell1.innerHTML = `<a href="/fluids.html?freq=${item.id.toUpperCase()}&lang=${locale}">`+ids + '</a> ' + `<span class="txt freq-guide">${localeText}</span>`;

        const previousItem = previousItemData.fluid[index] || {};
        const difference = item.size - (previousItem.size || 0);
        const differenceText = difference > 0 ? `+${difference}` : difference;
        cell2.innerHTML = `${item.size.toLocaleString()} <span class="diff-value ${difference > 0 ? 'add' : difference < 0 ? 'sub' : 'zero'}">${difference==0?"±":""}${differenceText.toLocaleString()}</span>`;
        cell2.classList.add('right-align');
    });

    // 現在のデータを保存
    previousItemData.fluid = data;
}
async function fetchEnergy() {
    const response = await fetch('/api/list/energy_frequency.json');
    const data = await response.json();
    const table = document.getElementById('energy-list').getElementsByTagName('tbody')[0];

    // 行数を調整
    while (table.rows.length < data.length) {
        table.insertRow();
    }
    while (table.rows.length > data.length) {
        table.deleteRow(-1);
    }
    // 行を上から書き換え
    data.forEach((item, index) => {
        const row = table.rows[index];
        let cell1 = row.cells[0];
        let cell2 = row.cells[1];
        if (!cell1) cell1 = row.insertCell(0);
        if (!cell2) cell2 = row.insertCell(1);

        const ids = item.id.split(',').map(id => `<div class="freq ${id}"></div>`).join('');
        const localeText = item.id.split(',').map(id => localeColour[locale][id] || id).join(', ');
        cell1.innerHTML = `<span>`+ids + '</span> ' + `<span class="txt freq-guide">${localeText}</span>`;

        const previousItem = previousItemData.energy[index] || {};
        const difference = Math.trunc((item.value - (previousItem.value || 0))*((performance.now()-previousEnergyTimestamp)/1000)/20);
        const differenceText = difference > 0 ? `+${difference.toLocaleString()}` : difference;
        cell2.innerHTML = `${item.value.toLocaleString()} <span style="width:100px" class="diff-value ${difference > 0 ? 'add' : difference < 0 ? 'sub' : 'zero'}">${difference==0?"±":""}${differenceText.toLocaleString()}RF/t</span>`;
        cell2.classList.add('right-align');
    });
    previousEnergyTimestamp=performance.now();
    // 現在のデータを保存
    previousItemData.energy = data;
}
async function fetchClients() {
    const response = await fetch('/api/list/clients.json');
    const data = await response.json();
    const list = document.getElementById('host-list');
    list.innerHTML="";
    data.forEach((item, index) => {
        let li=element=document.createElement("li");
        let span=element=document.createElement("span");
        span.innerText=item.name;
        li.appendChild(span);
        span=element=document.createElement("span");
        span.innerText=" "+item.sync+"ms";
        li.appendChild(span);
        list.appendChild(li);
    });
}
async function fetchData() {
    await Promise.all([fetchItem(),fetchFluid(),fetchEnergy(),fetchClients()]);
}
window.onload = async function () {
    await fetchData();
    setInterval(fetchData, 1000); // 1秒毎にfetchDataを実行
};

// Resizable table container
const resizable = document.querySelector('.resizable');
const resizer = document.querySelector('.resizer');

resizer.addEventListener('mousedown', function (e) {
    document.addEventListener('mousemove', resize);
    document.addEventListener('mouseup', stopResize);
});

function resize(e) {
    resizable.style.height = `${e.clientY - resizable.offsetTop}px`;
}

function stopResize() {
    document.removeEventListener('mousemove', resize);
    document.removeEventListener('mouseup', stopResize);
}
