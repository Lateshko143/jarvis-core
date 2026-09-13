import requests
import feedparser

def get_market_summary():
    try:
        url = "https://query1.finance.yahoo.com/v8/finance/chart/%5ENSEI"
        headers = {'User-Agent': 'Mozilla/5.0'}
        r = requests.get(url, headers=headers, timeout=5).json()
        meta = r['chart']['result'][0]['meta']
        price = meta['regularMarketPrice']
        prev_close = meta['chartPreviousClose']
        change = round(price - prev_close, 2)
        pct = round((change / prev_close) * 100, 2)
        return f"Nifty 50 is currently at {price} ({'+' if change > 0 else ''}{pct}%)."
    except Exception:
        return "Market feeds unreachable hain sir."

def get_live_news():
    try:
        # Google News Live India RSS Feed
        feed = feedparser.parse("https://news.google.com/rss?hl=hi&gl=IN&ceid=IN:hi")
        if feed.entries:
            headlines = [entry.title.split(" - ")[0] for entry in feed.entries[:2]]
            return "Aaj ki badi khabrein: " + ". Aur doosri khabar: ".join(headlines)
        return "Filhal koi naya news update nahi mila sir."
    except Exception:
        return "News feed connect nahi ho payi sir."

def web_search(query):
    try:
        feed = feedparser.parse(f"https://news.google.com/rss/search?q={requests.utils.quote(query)}&hl=hi&gl=IN&ceid=IN:hi")
        if feed.entries:
            return feed.entries[0].title.split(" - ")[0]
        return "Search data available nahi hai sir."
    except Exception:
        return "Search network timeout, sir."
