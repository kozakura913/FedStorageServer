
const enumColour = {
    white: {ja:"白",en:"White"},
    orange: {ja:"橙",en:"Orange"},
    magenta: {ja:"赤紫",en:"Magenta"},
    light_blue: {ja:"空",en:"Light Blue"},
    yellow: {ja:"黄",en:"Yellow"},
    lime: {ja:"黄緑",en:"Lime"},
    pink: {ja:"桃",en:"Pink"},
    gray: {ja:"灰",en:"Gray"},
    light_gray: {ja:"薄灰",en:"Light Gray"},
    cyan: {ja:"青緑",en:"Cyan"},
    purple: {ja:"紫",en:"Purple"},
    blue: {ja:"青",en:"Blue"},
    brown: {ja:"茶",en:"Brown"},
    green: {ja:"緑",en:"Green"},
    red: {ja:"赤",en:"Red"},
    black: {ja:"黒",en:"Black"},
};


const localeText = {
    ja: {
        itemInfoTitle: "アイテム情報",
        itemDetailInfoTitle: "チャンネル内アイテム情報",
        channelHeader: "チャンネル",
        queueHeader: "待ちキュー数",
        fluidInfoTitle: "液体情報",
        fluidChannelHeader: "チャンネル",
        fluidNameHeader: "液体名",
        fluidAmountHeader: "液体量",
        queueIdHeader: "順序番号",
        nameHeader: "アイテムID",
        amountHeader: "量"
    },
    en: {
        itemInfoTitle: "Item Information",
        itemDetailInfoTitle: "Item Detail Infomation",
        channelHeader: "Channel",
        queueHeader: "Queue Size",
        fluidInfoTitle: "Fluid Information",
        fluidChannelHeader: "Channel",
        fluidNameHeader: "Fluid Name",
        fluidAmountHeader: "Fluid Amount",
        queueIdHeader: "Queue num",
        nameHeader: "Item ID",
        amountHeader: "Amount"
    }
};


const locale = new URLSearchParams(window.location.search).get('lang') || 'en';

function switchLanguage() {
    const url = new URL(window.location.href);
    const newLang = locale === 'ja' ? 'en' : 'ja';
    url.searchParams.set('lang', newLang);
    window.location.href = url.toString();
}


function ReturnToHome() {
    const url = new URL(window.location.href);
    const lang = url.searchParams.get('lang') || 'en';
    window.location.href = `/?lang=${lang}`;
}

