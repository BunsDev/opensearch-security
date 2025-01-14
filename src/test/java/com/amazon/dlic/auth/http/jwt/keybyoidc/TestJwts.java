/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.amazon.dlic.auth.http.jwt.keybyoidc;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.logging.log4j.util.Strings;

class TestJwts {
    static final String ROLES_CLAIM = "roles";
    static final Set<String> TEST_ROLES = ImmutableSet.of("role1", "role2");
    static final String TEST_ROLES_STRING = Strings.join(TEST_ROLES, ',');

    static final String TEST_AUDIENCE = "TestAudience";

    static final String MCCOY_SUBJECT = "Leonard McCoy";

    static final String TEST_ISSUER = "TestIssuer";

    static final JwtToken MC_COY = create(MCCOY_SUBJECT, TEST_AUDIENCE, TEST_ISSUER, ROLES_CLAIM, TEST_ROLES_STRING);

    static final JwtToken MC_COY_2 = create(MCCOY_SUBJECT, TEST_AUDIENCE, TEST_ISSUER, ROLES_CLAIM, TEST_ROLES_STRING);

    static final JwtToken MC_COY_NO_AUDIENCE = create(MCCOY_SUBJECT, null, TEST_ISSUER, ROLES_CLAIM, TEST_ROLES_STRING);

    static final JwtToken MC_COY_NO_ISSUER = create(MCCOY_SUBJECT, TEST_AUDIENCE, null, ROLES_CLAIM, TEST_ROLES_STRING);

    static final JwtToken MC_COY_EXPIRED = create(
        MCCOY_SUBJECT,
        TEST_AUDIENCE,
        TEST_ISSUER,
        ROLES_CLAIM,
        TEST_ROLES_STRING,
        JwtConstants.CLAIM_EXPIRY,
        10
    );

    static final String MC_COY_SIGNED_OCT_1 = createSigned(MC_COY, TestJwk.OCT_1);

    static final String MC_COY_SIGNED_OCT_2 = createSigned(MC_COY_2, TestJwk.OCT_2);

    static final String MC_COY_SIGNED_NO_AUDIENCE_OCT_1 = createSigned(MC_COY_NO_AUDIENCE, TestJwk.OCT_1);
    static final String MC_COY_SIGNED_NO_ISSUER_OCT_1 = createSigned(MC_COY_NO_ISSUER, TestJwk.OCT_1);

    static final String MC_COY_SIGNED_OCT_1_INVALID_KID = createSigned(MC_COY, TestJwk.FORWARD_SLASH_KID_OCT_1);

    static final String MC_COY_SIGNED_RSA_1 = createSigned(MC_COY, TestJwk.RSA_1);

    static final String MC_COY_SIGNED_RSA_X = createSigned(MC_COY, TestJwk.RSA_X);

    static final String MC_COY_EXPIRED_SIGNED_OCT_1 = createSigned(MC_COY_EXPIRED, TestJwk.OCT_1);

    static class NoKid {
        static final String MC_COY_SIGNED_RSA_1 = createSignedWithoutKeyId(MC_COY, TestJwk.RSA_1);
        static final String MC_COY_SIGNED_RSA_2 = createSignedWithoutKeyId(MC_COY, TestJwk.RSA_2);
        static final String MC_COY_SIGNED_RSA_X = createSignedWithoutKeyId(MC_COY, TestJwk.RSA_X);
    }

    static class PeculiarEscaping {
        static final String MC_COY_SIGNED_RSA_1 = createSignedWithPeculiarEscaping(MC_COY, TestJwk.RSA_1);
    }

    static JwtToken create(String subject, String audience, String issuer, Object... moreClaims) {
        JwtClaims claims = new JwtClaims();

        claims.setSubject(subject);
        if (audience != null) {
            claims.setAudience(audience);
        }
        if (issuer != null) {
            claims.setIssuer(issuer);
        }

        if (moreClaims != null) {
            for (int i = 0; i < moreClaims.length; i += 2) {
                claims.setClaim(String.valueOf(moreClaims[i]), moreClaims[i + 1]);
            }
        }

        JwtToken result = new JwtToken(claims);

        return result;
    }

    static String createSigned(JwtToken baseJwt, JsonWebKey jwk) {
        return createSigned(baseJwt, jwk, JwsUtils.getSignatureProvider(jwk));
    }

    static String createSigned(JwtToken baseJwt, JsonWebKey jwk, JwsSignatureProvider signatureProvider) {
        JwsHeaders jwsHeaders = new JwsHeaders();
        JwtToken signedToken = new JwtToken(jwsHeaders, baseJwt.getClaims());

        jwsHeaders.setKeyId(jwk.getKeyId());

        return new JoseJwtProducer().processJwt(signedToken, null, signatureProvider);
    }

    static String createSignedWithoutKeyId(JwtToken baseJwt, JsonWebKey jwk) {
        JwsHeaders jwsHeaders = new JwsHeaders();
        JwtToken signedToken = new JwtToken(jwsHeaders, baseJwt.getClaims());

        return new JoseJwtProducer().processJwt(signedToken, null, JwsUtils.getSignatureProvider(jwk));
    }

    static String createSignedWithPeculiarEscaping(JwtToken baseJwt, JsonWebKey jwk) {
        JwsSignatureProvider signatureProvider = JwsUtils.getSignatureProvider(jwk);
        JwsHeaders jwsHeaders = new JwsHeaders();
        JwtToken signedToken = new JwtToken(jwsHeaders, baseJwt.getClaims());

        // Depends on CXF not escaping the input string. This may fail for other frameworks or versions.
        jwsHeaders.setKeyId(jwk.getKeyId().replace("/", "\\/"));

        return new JoseJwtProducer().processJwt(signedToken, null, signatureProvider);
    }

    static String createMcCoySignedOct1(long nbf, long exp) {
        JwtToken jwt_token = create(
            MCCOY_SUBJECT,
            TEST_AUDIENCE,
            TEST_ISSUER,
            ROLES_CLAIM,
            TEST_ROLES_STRING,
            JwtConstants.CLAIM_NOT_BEFORE,
            nbf,
            JwtConstants.CLAIM_EXPIRY,
            exp
        );

        return createSigned(jwt_token, TestJwk.OCT_1);
    }

}
