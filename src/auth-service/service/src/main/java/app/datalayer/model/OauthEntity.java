package app.datalayer.model;


import app.domain.model.ProviderEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "oauth_identities",
        uniqueConstraints = @UniqueConstraint(
            columnNames = {"provider", "provider_user_id"}
        ),
        indexes = @Index(name = "idx_oauth_identities_user", columnList = "user_id")
)
public class OauthEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_oauth_identities_users",
                    value = ConstraintMode.CONSTRAINT
            ),
            nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderEnum provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;
}
