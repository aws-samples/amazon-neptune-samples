create schema chatbot_dw;

create table chatbot_dw.author_summary as
select a.name,count(pa.*) as post_count
from chatbot_external.authors as a 
inner join chatbot_external.post_authors as pa on a.author_id = pa.author_id
group by a.name
order by count(pa.*) desc;
