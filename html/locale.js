
const localeColour = {
    ja : {
        white: "白",
        orange: "橙",
        magenta: "赤紫",
        light_blue: "空",
        yellow: "黄",
        lime: "黄緑",
        pink: "桃",
        gray: "灰",
        light_gray: "薄灰",
        cyan: "青緑",
        purple: "紫",
        blue: "青",
        brown: "茶",
        green: "緑",
        red: "赤",
        black: "黒"
    },
    en : {
        white: "White",
        orange: "Orange",
        magenta: "Magenta",
        light_blue: "Light Blue",
        yellow: "Yellow",
        lime: "Lime",
        pink: "Pink",
        gray: "Gray",
        light_gray: "Light Gray",
        cyan: "Cyan",
        purple: "Purple",
        blue: "Blue",
        brown: "Brown",
        green: "Green",
        red: "Red",
        black: "Black"
    }
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
        modIdHeader: "Mod ID",
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
        modIdHeader: "Mod ID",
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
