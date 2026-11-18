package app.datalayer.repository;

import app.datalayer.model.OauthEntity;
import app.domain.model.ProviderEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OauthRepository extends JpaRepository<OauthEntity, UUID> {

    Optional<OauthEntity> findByProviderAndProviderUserId(ProviderEnum provider, String providerUserId);
}
