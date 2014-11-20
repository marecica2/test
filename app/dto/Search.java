package dto;

import java.util.List;

import javax.persistence.Query;

import play.db.jpa.JPA;

public class Search
{
    public static List<String> tags(String search)
    {
        Query query = JPA.em().createNativeQuery("select distinct term from tags where term like :search order by term");
        query.setParameter("search", "%" + search + "%");
        List<String> result = query.getResultList();
        return result;
    }
}
