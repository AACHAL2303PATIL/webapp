package ai.elimu.dao.jpa;

import ai.elimu.model.contributor.LetterSoundCorrespondenceContributionEvent;
import ai.elimu.dao.LetterSoundCorrespondenceContributionEventDao;
import ai.elimu.model.content.LetterSoundCorrespondence;
import ai.elimu.model.contributor.Contributor;
import java.util.List;
import org.springframework.dao.DataAccessException;

public class LetterSoundCorrespondenceContributionEventDaoJpa extends GenericDaoJpa<LetterSoundCorrespondenceContributionEvent> implements LetterSoundCorrespondenceContributionEventDao {

    @Override
    public List<LetterSoundCorrespondenceContributionEvent> readAllOrderedByTimeDesc() throws DataAccessException {
        return em.createQuery(
            "SELECT e " + 
            "FROM LetterSoundCorrespondenceContributionEvent e " +
            "ORDER BY e.time DESC")
            .getResultList();
    }

    @Override
    public List<LetterSoundCorrespondenceContributionEvent> readAll(LetterSoundCorrespondence letterSoundCorrespondence) throws DataAccessException {
        return em.createQuery(
            "SELECT e " + 
            "FROM LetterSoundCorrespondenceContributionEvent e " +
            "WHERE e.letterSoundCorrespondence = :letterSoundCorrespondence " + 
            "ORDER BY e.time DESC")
            .setParameter("letterSoundCorrespondence", letterSoundCorrespondence)
            .getResultList();
    }

    @Override
    public List<LetterSoundCorrespondenceContributionEvent> readAll(Contributor contributor) throws DataAccessException {
        return em.createQuery(
            "SELECT e " + 
            "FROM LetterSoundCorrespondenceContributionEvent e " +
            "WHERE e.contributor = :contributor " + 
            "ORDER BY e.time DESC")
            .setParameter("contributor", contributor)
            .getResultList();
    }

    @Override
    public Long readCount(Contributor contributor) throws DataAccessException {
        return (Long) em.createQuery("SELECT COUNT(e) " +
            "FROM LetterSoundCorrespondenceContributionEvent e " +
            "WHERE e.contributor = :contributor")
            .setParameter("contributor", contributor)
            .getSingleResult();
    }
}
