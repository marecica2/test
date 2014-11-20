DROP materialized view search_index;
CREATE MATERIALIZED VIEW search_index AS 
SELECT listing.uuid,
       listing.title,
       listing.tags,
       setweight(to_tsvector('english'::regconfig, listing.title), 'A') || 
       setweight(to_tsvector('simple', listing.tags), 'B') ||
       setweight(to_tsvector('simple', concat(users.firstName, ' ', users.lastName)), 'C') as document
FROM listing
JOIN users ON users.id = listing.user_id
GROUP BY listing.uuid, listing.title,listing.tags, users.uuid, users.firstName, users.lastName;

/* 

CREATE INDEX idx_fts_search ON search_index USING gin(document);

SELECT title, tags
FROM search_index
WHERE document @@ to_tsquery('java') as sss
ORDER BY ts_rank(document, to_tsquery('sexxx')) DESC;


CREATE EXTENSION unaccent;
DROP materialized view tags;
CREATE MATERIALIZED VIEW tags AS 
select distinct regexp_split_to_table(unaccent(lower(tags)),',') as term from listing 
union all
select distinct regexp_split_to_table(unaccent(lower(title)),'\s') as term from listing 
order by term


select distinct term from tags where term like '%ava%'

*/