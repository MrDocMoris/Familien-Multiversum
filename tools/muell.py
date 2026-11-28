"""Holt die Müllabfuhr-Termine für einen Ortsteil der Gemeinde Nörvenich von RegioEntsorgung
und speichert sie als JSON für die App (daten/muell-<ortsteil>.json).

Aufruf:  python tools/muell.py Irresheim
Ablauf wie im Online-Abfallkalender: Ort -> Straße -> Hausnummer -> iCal-Download.
"""
import json, re, sys, datetime, os
from html.parser import HTMLParser
import requests

API = "https://tonnen.regioentsorgung.de/WasteManagementRegioentsorgung/WasteManagementServlet"
HEAD = {"User-Agent": "Mozilla/5.0 (Familien-Multiversum Abfallkalender)"}
CITY = "Nörvenich"


class Opts(HTMLParser):
    def __init__(self):
        super().__init__(); self.o = {}; self.cur = None; self.val = None; self.txt = []
    def _fin(self):
        if self.cur is not None and self.val is not None:
            self.o[self.cur].append((self.val, " ".join(t.strip() for t in self.txt).strip()))
        self.val = None; self.txt = []
    def handle_starttag(self, tag, a):
        a = dict(a)
        if tag == "select" and "name" in a: self._fin(); self.cur = a["name"]; self.o.setdefault(self.cur, [])
        elif tag == "option" and self.cur is not None: self._fin(); self.val = a.get("value", "")
    def handle_data(self, d):
        if self.val is not None: self.txt.append(d)
    def handle_endtag(self, tag):
        if tag in ("option", "select"): self._fin()
        if tag == "select": self.cur = None


def options(html, name):
    p = Opts(); p.feed(html); p._fin()
    return [t or v for v, t in p.o.get(name, []) if (t or v) and not (t or v).startswith("Bitte")]


def post(s, **data):
    r = s.post(API, headers=HEAD, data={"ApplicationName": "com.athos.kd.regioentsorgung.CheckAbfuhrtermineModel", **data}, timeout=60)
    r.raise_for_status(); r.encoding = "utf-8"; return r.text


def streets(s):
    r = s.get(API, headers=HEAD, params={"SubmitAction": "wasteDisposalServices"}, timeout=60); r.raise_for_status(); r.encoding = "utf-8"
    city = next(c for c in options(r.text, "Ort") if c.replace("\xa0", " ").strip() == CITY)
    html = post(s, SubmitAction="CITYCHANGED", Ort=city, Strasse="", Hausnummer="")
    return city, options(html, "Strasse")


def ical(city, street, year):
    s = requests.Session()
    s.get(API, headers=HEAD, params={"SubmitAction": "wasteDisposalServices"}, timeout=60)
    post(s, SubmitAction="CITYCHANGED", Ort=city, Strasse="", Hausnummer="")
    html = post(s, SubmitAction="STREETCHANGED", Ort=city, Strasse=street, Hausnummer="")
    hn = options(html, "Hausnummer")
    post(s, SubmitAction="forward", Ort=city, Strasse=street, Hausnummer=hn[0] if hn else "", Zeitraum=f"Jahresübersicht {year}")
    s.post(API, headers=HEAD, data={"ApplicationName": "com.athos.kd.regioentsorgung.AbfuhrTerminModel", "SubmitAction": "forward"}, timeout=60)
    data = {"ApplicationName": "com.athos.kd.regioentsorgung.AbfuhrTerminDownloadModel", "ICalErinnerung": "keine Erinnerung",
            "ICalZeit": "06:00 Uhr", "SubmitAction": "filedownload_ICAL"}
    for i in range(1, 13): data[f"ContainerGewaehlt_{i}"] = "on"
    r = s.post(API, headers=HEAD, data=data, timeout=60); r.raise_for_status(); r.encoding = "utf-8"
    return r.text


def parse_ics(text):
    text = re.sub(r"\r?\n[ \t]", "", text)
    out = []
    for ev in re.findall(r"BEGIN:VEVENT(.*?)END:VEVENT", text, re.S):
        d = re.search(r"DTSTART[^:\n]*:(\d{4})(\d{2})(\d{2})", ev)
        m = re.search(r"\nSUMMARY[^:\n]*:(.*)", ev)
        if d and m:
            out.append({"date": f"{d[1]}-{d[2]}-{d[3]}", "name": m[1].strip().replace("\\,", ",").replace("\\;", ";")})
    return out


def osm_streets(ortsteil):
    """Straßennamen im Ortsteil laut OpenStreetMap (Umkreis um den Ortskern)."""
    q = f"""[out:json][timeout:60];
node["place"]["name"="{ortsteil}"](50.70,6.50,50.90,6.75)->.c;
way(around.c:1200)["highway"]["name"];
out tags;"""
    oh = {"User-Agent": "FamilienMultiversum/1.0 (https://github.com/MrDocMoris/Familien-Multiversum)", "Accept": "application/json"}
    for url in ("https://overpass-api.de/api/interpreter", "https://overpass.private.coffee/api/interpreter", "https://maps.mail.ru/osm/tools/overpass/api/interpreter"):
        try:
            r = requests.post(url, data={"data": q}, headers=oh, timeout=120); r.raise_for_status()
            return {e["tags"]["name"] for e in r.json()["elements"]}
        except Exception as e:
            print("Overpass-Fehler:", url, e)
    return set()


norm = lambda x: " ".join(x.replace("\xa0", " ").split()).casefold()


def main():
    ortsteil = sys.argv[1] if len(sys.argv) > 1 else "Irresheim"
    s = requests.Session()
    city, all_streets = streets(s)
    print(f"{len(all_streets)} Straßen in {CITY}: {[norm(x) for x in all_streets]}")
    fixed = f"tools/strassen-{ortsteil.lower()}.json"
    if os.path.exists(fixed):
        with open(fixed, encoding="utf-8") as f:
            osm = {norm(n) for n in json.load(f)["strassen"]}
        print(f"Feste Straßenliste: {len(osm)} Straßen ({fixed})")
    else:
        osm = {norm(n) for n in osm_streets(ortsteil)}
        print(f"OpenStreetMap: {len(osm)} Straßen rund um {ortsteil}")
    mine = [x for x in all_streets if norm(x) in osm or ortsteil.lower() in x.lower()]
    print(f"Treffer für {ortsteil}: {mine}")
    if not mine:
        sys.exit(f"Keine Straße mit '{ortsteil}' gefunden")
    today = datetime.date.today()
    years = [today.year] + ([today.year + 1] if today.month >= 10 else [])
    result = {"ort": f"{CITY}-{ortsteil}", "stand": today.isoformat(), "quelle": "RegioEntsorgung", "strassen": {}}
    for st in mine:
        dates = []
        for y in years:
            try:
                ev = parse_ics(ical(city, st, y))
                print(f"  {st} {y}: {len(ev)} Termine, Arten: {sorted({e['name'] for e in ev})}")
                dates += ev
            except Exception as e:
                print(f"  {st} {y}: Fehler {e}")
        uniq = sorted({(d["date"], d["name"]) for d in dates})
        result["strassen"][st] = [{"date": d, "name": n} for d, n in uniq]
    if not any(result["strassen"].values()):
        sys.exit("Keine Termine erhalten – alte Daten bleiben unverändert")
    os.makedirs("daten", exist_ok=True)
    fn = f"daten/muell-{ortsteil.lower()}.json"
    with open(fn, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)
    print("gespeichert:", fn)


if __name__ == "__main__":
    main()
