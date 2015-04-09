/* search index */
DROP materialized view IF EXISTS search_index CASCADE;
CREATE MATERIALIZED VIEW search_index AS 
SELECT listing.uuid,
       listing.title,
       users.firstname,
       users.lastname,
       users.avatarUrl,
       listing.category,
       listing.privacy,
       listing.charging,
       listing.price,
       listing.currency,
       listing.imageUrl,
       listing.tags,
       listing.type,
       listing.firstFree,
       count(r.stars) as ratingStars,
       sum(r.stars) / count(r.stars)::float as ratingAvg,
       account.type as account,
       users.login as login,
       listing.description,
       listing.language,
       setweight(to_tsvector('english'::regconfig, listing.title), 'A') || 
       setweight(to_tsvector('simple', listing.tags), 'B') ||
       setweight(to_tsvector('simple', listing.description), 'C') ||
       setweight(to_tsvector('simple', concat(users.firstName, ' ', users.lastName, ' ', users.login)), 'D') as document
FROM listing
JOIN users ON users.id = listing.user_id
LEFT OUTER JOIN rating r ON r.objectuuid = listing.uuid
JOIN account ON users.account_id = account.id and account.type = 'publisher' where deleted is null
GROUP BY listing.uuid, listing.title, listing.description, users.firstname, users.lastname, users.avatarUrl, listing.category,listing.privacy,listing.charging,listing.price,listing.currency,listing.imageUrl,listing.tags,
listing.type,listing.firstFree,account.type,users.login, listing.language;
CREATE INDEX idx_fts_search ON search_index USING gin(document);
REFRESH MATERIALIZED VIEW search_index;

/* tags view and refresh */
DROP materialized view IF EXISTS tags CASCADE;
CREATE MATERIALIZED VIEW tags AS 
select distinct regexp_split_to_table((lower(tags)),',') as term from listing 
order by term;
CREATE INDEX tags_search ON tags (term);
REFRESH MATERIALIZED VIEW tags;

/* clear tables */
DROP TABLE IF EXISTS ratingvote CASCADE;
DROP TABLE IF EXISTS comment_comment CASCADE;
DROP TABLE IF EXISTS comment_reply CASCADE;
DROP TABLE IF EXISTS comment_comment_reply CASCADE;
DROP TABLE IF EXISTS comment_fileupload CASCADE;
DROP TABLE IF EXISTS activity CASCADE;
DROP TABLE IF EXISTS rating CASCADE;
DROP TABLE IF EXISTS event_comment_fileupload CASCADE;
DROP TABLE IF EXISTS event_comment CASCADE;
DROP TABLE IF EXISTS fileupload CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS message CASCADE;
DROP TABLE IF EXISTS event CASCADE;
DROP TABLE IF EXISTS chat_feed CASCADE;
DROP TABLE IF EXISTS attendance CASCADE;



/* search index query example */
SELECT title,firstname, lastname, tags
FROM search_index
WHERE document @@ to_tsquery('english', 'lopez')
ORDER BY ts_rank(document, to_tsquery('english', 'lopez')) DESC;


/*
CREATE EXTENSION unaccent;
DROP materialized view tags;
CREATE MATERIALIZED VIEW tags AS 
select distinct regexp_split_to_table(unaccent(lower(tags)),',') as term from listing 
union all
select distinct regexp_split_to_table(unaccent(lower(title)),'\s') as term from listing 
order by term

select distinct term from tags where term like '%ava%'
 */


delete from activity where user_id = 6922;
delete from activity where customer_id = 6922;
delete from activity where event_id in (select id from event where user_id = 6922);
delete from message where fromuser_id = 6922;
delete from message where owner_id = 6922;
delete from message where touser_id = 6922;
delete from attendance where user_id = 6922;
delete from attendance where customer_id = 6922;
delete from event where user_id = 6922;
delete from event where customer_id = 6922;
delete from comment_comment_reply where comment_id in (select id from comment where user_id = 6922);
delete from comment_comment_reply where replies_id in (select id from comment_reply where user_id = 6922);
delete from comment where user_id = 6922;
delete from comment_reply where user_id = 6922;
delete from fileupload where owner_id  = 6922;
delete from listing where user_id = 6922;
delete from contact where user_id = 6922;
delete from contact where contact_id = 6922;
delete from ratingvote where user_id = 6922;
delete from rating where user_id = 6922;
delete from users where id = 6922;