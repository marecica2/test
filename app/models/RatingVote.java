package models;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class RatingVote extends Model
{
    @ManyToOne(cascade = CascadeType.ALL)
    public User user;

    @ManyToOne
    public Rating rating;

    public Integer vote;

    public static RatingVote getByUuid(String uuid)
    {
        return RatingVote.find("uuid = ?", uuid).first();
    }
}
