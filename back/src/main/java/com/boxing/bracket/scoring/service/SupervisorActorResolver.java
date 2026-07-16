package com.boxing.bracket.scoring.service;

import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.domain.AuthSessionContext;
import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.auth.exception.AuthenticationRequiredException;
import com.boxing.bracket.user.domain.UserRole;

final class SupervisorActorResolver {

    private SupervisorActorResolver() {
    }

    static Long resolve(Long requestActorId) {
        AuthSession session = AuthSessionContext.get();
        if (session == null) {
            if (requestActorId == null) {
                throw new AuthenticationRequiredException();
            }
            return requestActorId;
        }
        if (session.getRole() != UserRole.SUPERVISOR) {
            throw new AccessDeniedException("SUPERVISOR_REQUIRED");
        }
        if (requestActorId != null && !session.getAccountId().equals(requestActorId)) {
            throw new AccessDeniedException("ACTOR_ID_MISMATCH");
        }
        return session.getAccountId();
    }
}
