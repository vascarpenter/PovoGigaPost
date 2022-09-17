# PovoGigaPost

https://github.com/

### povo のデータ使用量を日々postしていくandroidアプリ

- これまで softbank系SIMを契約していた
  - スピードは速いが、田舎だとやはり入らない地域が出てくる
  - rakutenよりはましだが..
  - やむなく docomo系か au系かを選ぶが、povoはahamoと違って安くて使う量だけ課金していくタイプ
  - 試しにはじめてみたが、日に0.5-1.5GB使う生活をしていると、残量や曜日、日による使用量を目で見たくなる
  
  - povoアプリはデータ使用量を外に出せず、また公式も残量のAPIなど用意してくれるわけではない
  - 手入力になるが、例えばアプリを作って職場に着いたら確認してpost, 自宅に帰ったら確認してpostなどならそんなに負担にならない

### まず ADW に Tableを作っておく
```sql
  CREATE TABLE "ADMIN"."POVOGIGA" 
   (	"DATE" DATE NOT NULL ENABLE, 
	"GIGALEFT" VARCHAR2(10 BYTE) COLLATE "USING_NLS_COMP" NOT NULL ENABLE, 
	"MEMO" VARCHAR2(100 BYTE) COLLATE "USING_NLS_COMP", 
	"GIGAUSED" NUMBER, 
	"DATEONLY" VARCHAR2(20 BYTE) COLLATE "USING_NLS_COMP", 
	"DAYONLY" VARCHAR2(20 BYTE) COLLATE "USING_NLS_COMP"
   )  DEFAULT COLLATION "USING_NLS_COMP" SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 10 MAXTRANS 255 
 COLUMN STORE COMPRESS FOR QUERY HIGH ROW LEVEL LOCKING LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "DATA" ;
```
### APIを提供するserver sideソースの例(rust actix_web)
- apikeyはapiへのアクセス文字列を持っているものとする

```rust
use actix_web::{get, HttpResponse, post, Responder, web};
use r2d2::Pool;
use r2d2_oracle::OracleConnectionManager;
use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};

use super::index::MyError;
use crate::GlobalData;


#[derive(Deserialize)]
pub struct PovoAPIInfo {
	apikey: String,
}

#[derive(Serialize)]
pub struct PovoInfo {
    date: String,
	gigaleft: String,
	memo: Option<String>,	
}

#[get("/get")]
pub async fn povoapi(db: web::Data<Pool<OracleConnectionManager>>,
	params: web::Query<PovoAPIInfo>,
	globals: web::Data<Arc<Mutex<GlobalData>>>,
)  -> Result<HttpResponse, MyError>  {
    let conn = db.get()?;
    let mut vec:Vec<PovoInfo> = Vec::new();

    if params.apikey == globals.lock().unwrap().apikey 
	{
        let sql = r##"SELECT TO_CHAR("DATE",'YY/MM/DD HH24MI'),GIGALEFT,MEMO FROM (SELECT * FROM POVOGIGA ORDER BY "DATE" DESC) t WHERE ROWNUM<14 ORDER BY t."DATE""##;
        let rows = conn.query_as::<(String, String, Option<String>)>(sql, &[])?;

        for row_result in rows {
            if let Ok((e0, e1, e2)) = row_result {
                let entry = PovoInfo {
                    date: e0,
                    gigaleft: e1,
                    memo: e2,
                };
                vec.push(entry);
            }
        }
    }
    return Ok(HttpResponse::Ok()
		.content_type("application/json")
		.json(&vec));
}

#[derive(Deserialize)]
pub struct PovoPostInfo {
	apikey: String,
    date: String,
	gigaleft: String,
	memo: String,	
}

#[post("/post")]
pub async fn povoapipost(db: web::Data<Pool<OracleConnectionManager>>,
	params: web::Json<PovoPostInfo>,
	globals: web::Data<Arc<Mutex<GlobalData>>>,
)  -> Result<HttpResponse, MyError>  {
	let mut apiok : String = "WRONG".to_string();
    let conn = db.get()?;

	if params.apikey == globals.lock().unwrap().apikey 
	{
		let sql = r##"INSERT INTO POVOGIGA ("DATE",GIGALEFT,MEMO) VALUES (TO_DATE(:1,'YYYYMMDD HH24MI'),:2,:3)"##;
		conn.execute(sql, &[&params.date,
			 &params.gigaleft,
			 &params.memo])?;
	   conn.commit()?;
	   apiok = "OK".to_string();
	}
	return Ok(HttpResponse::Ok()
		.content_type("text/html")
		.body(apiok));
}
```

### このandroidアプリをコンパイルする前に

- build.gradle :app から ３つの文字列を参照しているので 
- `~/.gradle/gradle.properties` に追加しておく
  ```
  # 自分のサイトにあった設定に差し替えてね
  povopostapiurl=https://ogehage.tk/post
  povogetapiurl=https://ogehage.tk/get?apikey=THEKEY
  povoapikey=THEKEY
  ```
- kotlin ソース内で参照してます
- 画面下にこれまでの投稿歴を表示し、上で投稿するパターン

### SQLで集計
- PL/SQLファンクションを定義
```sql
CREATE OR REPLACE PROCEDURE calcgigaused IS

    CURSOR cur IS
    SELECT
        "DATE" AS recdate,
        to_number(gigaleft) - to_number(LEAD(gigaleft) OVER(
            ORDER BY
                "DATE"
        )) AS used
    FROM
        povogiga;

BEGIN
    FOR rec IN cur LOOP
        UPDATE povogiga
        SET
            gigaused = rec.used
        WHERE
            "DATE" = rec.recdate;

    END LOOP;

    UPDATE povogiga
    SET
        dateonly = to_char("DATE", 'yyyy-MM-DD');

    UPDATE povogiga
    SET
        dayonly = to_char("DATE", 'DAY');

END;
```

- calcgiga.sql
```sql
SET FEEDBACK OFF

execute calcgigaused();
select dateonly as "DATE",Sum(gigaused) AS GIGA from povogiga group by dateonly order by dateonly;
exit
```

### 得られた結果
- `sqlplus -s admin/hoge@hoge_high @calcgiga.sql`
```
DATE			   GIGA
-------------------- ----------
2022-09-10		    .63
2022-09-11		    .22
2022-09-12		    .71
2022-09-13		    .68
2022-09-14		    .59
2022-09-15		   1.16
2022-09-16		    .99
```