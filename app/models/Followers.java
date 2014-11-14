package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name = "followers")
public class Followers extends Model
{
    @ManyToOne
    public User followSource;

    @ManyToOne
    public User followTarget;

    public Date created;
    public String uuid;

    public static Followers get(User followSource, User followTarget)
    {
        return Followers.find("followSource = ? and followTarget = ? order by created desc", followSource, followTarget).first();
    }

    public static List<Followers> getFollowing(User followSource)
    {
        return Followers.find("followSource = ? order by created desc", followSource).fetch(100);
    }

    public static List<Followers> getFollowers(User followTarget)
    {
        return Followers.find("followTarget = ? order by created desc", followTarget).fetch(100);
    }

}
