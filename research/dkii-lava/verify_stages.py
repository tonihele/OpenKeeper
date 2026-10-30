#!/usr/bin/env python3
import json,sys
from pathlib import Path
rows=[json.loads(s) for s in Path(sys.argv[1]).read_text().splitlines()]
results={}
for case in ('bump','bump_luminance','restore_from_bump','restore_from_luminance','bump_cached','luminance_cached'):
 calls=[r for r in rows if r['case']==case and r['event']=='texture_stage']
 summary=[r for r in rows if r['case']==case and r['event']=='summary'];assert len(summary)==1
 state={}
 for r in calls:state[r['stage'],r['type']]=r['value']
 if case in ('bump','bump_luminance'):
  assert state[0,1]==4 # MODULATE
  assert state[1,1]==(22 if case=='bump' else 23)
  assert state[2,1]==7 # ADD
  assert summary[0]['units']==3
  assert [state[1,i] for i in (7,8,9,10)]==[1065353216,0,0,1065353216] # identity bump matrix
 if case.startswith('restore'):
  assert state[1,1]==state[2,1]==1 # DISABLE
  assert summary[0]['units']==1
 if case.endswith('cached'):assert summary[0]['calls']==0
 results[case]=dict(calls=summary[0]['calls'],stages=summary[0]['units'],colour_ops={str(stage):value for (stage,t),value in state.items() if t==1},bump_matrix=[state.get((1,i)) for i in (7,8,9,10)])
print(json.dumps(results,indent=2))
