let previousItemData = [];
const freq = new URLSearchParams(window.location.search).get('freq');
// ロケールによって表示するテキストを変更する
window.addEventListener("load", function () {
    document.getElementById('item-info-title').innerText = localeText[locale].itemDetailInfoTitle;
    document.getElementById('modId-header').innerText = localeText[locale].modIdHeader;
    document.getElementById('name-header').innerText = localeText[locale].nameHeader;
    document.getElementById('amount-header').innerText = localeText[locale].amountHeader;

    const f = freq.toLocaleLowerCase();
    const ids = f.split(',').map(id => `<div class="freq ${id}"></div>`).join('');
    const text = f.split(',').map(id => localeColour[locale][id] || id).join(', ');
    document.getElementById('channel-title').innerHTML = ids + `<p class="txt freq-guide">${text}</p>`;;
});


async function fetchItem() {
    const url = new URL('/api/list/items.json', window.location.origin);
    url.searchParams.set('frequency', freq);
    const response = await fetch(url);
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
    var position = 1;
    data.forEach((item, index) => {
        const row = table.rows[index];
        let cell1 = row.cells[0];
        let cell2 = row.cells[1];
        let cell3 = row.cells[2];
        if (!cell1) cell1 = row.insertCell(0);
        if (!cell2) cell2 = row.insertCell(1);
        if (!cell3) cell3 = row.insertCell(2);
        const split=String(item.name).split(":");
        cell1.innerHTML = split[0];
        cell1.classList.add('right-align');
        cell2.innerHTML = split[1];
        cell3.innerHTML = item.count.toLocaleString();
        cell3.classList.add('right-align');
        position++;
    });

    // 現在のデータを保存
    previousItemData = data;
}

async function fetchData() {
    await fetchItem();
}

window.onload = function () {
    fetchData();
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
