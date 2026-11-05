import pandas as pd, json, re, sys
d=pd.read_excel(sys.argv[1],dtype=str).fillna('')
def clean(s): return re.sub(r'\s+',' ',str(s).replace('\t',' ')).strip()
d['NUMBER']=d['NUMBER'].map(clean); d['TITLE']=d['TITLE'].map(clean)
d['ARTIST']=d['ARTIST'].map(clean).replace('', 'Unknown Artist')
d['CATEGORY']=d['CATEGORY'].map(clean).replace('', 'Uncategorized')
def key(t): return re.sub(r'^[^0-9A-Za-z]+','',t).lower() or t.lower()
d['k']=d['TITLE'].map(key)
d=d.sort_values(['k','ARTIST']).reset_index(drop=True)
arts=sorted(d['ARTIST'].unique(), key=lambda a:(key(a),a))
ai={a:i for i,a in enumerate(arts)}
cats=sorted(d['CATEGORY'].unique()); ci={c:i for i,c in enumerate(cats)}
lines='\n'.join(f"{r.NUMBER}\t{r.TITLE}\t{ai[r.ARTIST]}\t{ci[r.CATEGORY]}" for r in d.itertuples())
out=json.dumps({'a':arts,'c':cats,'s':lines},ensure_ascii=False,separators=(',',':'))
open(sys.argv[2],'w').write(out.replace('</','<\\/'))
print(len(d),len(arts),cats,len(out))
