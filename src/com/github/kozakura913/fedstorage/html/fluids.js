let previousItemData = [];
const freq = new URLSearchParams(window.location.search).get('freq');
// ロケールによって表示するテキストを変更する
window.addEventListener("load", function () {
    document.getElementById('fluid-info-title').innerText = localeText[locale].fluidDetailInfoTitle;
    document.getElementById('name-header').innerText = localeText[locale].fluidNameHeader;
    document.getElementById('amount-header').innerText = localeText[locale].fluidAmountHeader;

    const f = freq.toLocaleLowerCase();
    const ids = f.split(',').map(id => `<div class="freq ${id}"></div>`).join('');
    const text = f.split(',').map(id => localeColour[locale][id] || id).join(', ');
    document.getElementById('channel-title').innerHTML = ids + `<p class="txt freq-guide">${text}</p>`;;
});

function sortBy(sort_by){
    const current_sort_by=new URLSearchParams(window.location.search).get('sort_by');
    const parms=new URLSearchParams(window.location.search);
    if(current_sort_by!=sort_by)parms.set("sort_by",sort_by);
    else parms.delete("sort_by");
    const url = new URL(window.location.pathname+"?"+parms,window.location.href);
    window.location.href = url.toString();
}

async function fetchItem() {
    const url = new URL('/api/list/fluids.json', window.location.origin);
    url.searchParams.set('frequency', freq);
    const response = await fetch(url);
    const data = await response.json();
    const table = document.getElementById('fluid-list').getElementsByTagName('tbody')[0];

    // 行数を調整
    while (table.rows.length < data.length) {
        table.insertRow();
    }
    while (table.rows.length > data.length) {
        table.deleteRow(-1);
    }
    const sort_by=new URLSearchParams(window.location.search).get('sort_by');
    if(sort_by==="count"){
        data.sort((a,b)=>b.count-a.count);
    }else if(sort_by==="name"){
        data.sort((a,b)=>{
            return (b.name>a.name)?-1:1;
        });
    }
    // 行を上から書き換え
    data.forEach((item, index) => {
        const row = table.rows[index];
        let cell1 = row.cells[0];
        let cell2 = row.cells[1];
        if (!cell1) cell1 = row.insertCell(0);
        if (!cell2) cell2 = row.insertCell(1);
        cell1.innerHTML = item.name;
        cell2.innerHTML = item.count.toLocaleString();
        cell2.classList.add('right-align');
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
