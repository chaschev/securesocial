/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service;

import com.google.common.base.Joiner;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.GeneratedKeys;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.VoidHandleCallback;
import org.skife.jdbi.v2.util.LongMapper;
import play.Application;
import scala.Option;
import securesocial.core.Identity;
import securesocial.core.IdentityId;
import securesocial.core.java.BaseUserService;
import securesocial.core.java.Token;

import java.util.*;

/**
 * A Sample SQL user service in Java
 * <p/>
 * Note: This is NOT suitable for a production environment and is provided only as a guide.
 * A real implementation would persist things in a database
 */
public class SqlUserService extends BaseUserService {
    private HashMap<String, Identity> users = new HashMap<String, Identity>();
    private HashMap<String, Token> tokens = new HashMap<String, Token>();
    private final DBI dbi;

    public SqlUserService(Application application) {
        super(application);

        Config conf = ConfigFactory.load();

        dbi = new DBI(
                conf.getString("sql.url"),
                conf.getString("sql.user"),
                conf.getString("sql.password")
        );
    }

    @Override
    public Identity doSave(Identity user) {
        users.put(user.identityId().userId() + user.identityId().providerId(), user);

        final Map<String, Object> userMap = flatMap(MongoUserService.userToMap(user));

        dbi.withHandle(new VoidHandleCallback() {
            @Override
            protected void execute(Handle h) throws Exception {
                StringBuilder sb = buildValues("insert into users ", userMap);

                GeneratedKeys<Long> keys = bindParams(h.createStatement(sb.toString()), userMap).executeAndReturnGeneratedKeys(LongMapper.FIRST);

                List<Long> keyList = keys.list(20);

                System.out.println(keyList);
            }
        });

        System.out.println(userMap);

        return user;
    }

    @Override
    public void doSave(Token token) {
        tokens.put(token.uuid, token);

        final Map<String, Object> tokenMap = MongoUserService.tokenToMap(token);

        dbi.withHandle(new VoidHandleCallback() {
            @Override
            protected void execute(Handle h) throws Exception {
                GeneratedKeys<Long> keys = bindParams(
                        h.createStatement(
                                buildValues("insert into tokens ", tokenMap).toString()),
                        tokenMap
                ).executeAndReturnGeneratedKeys(LongMapper.FIRST);

                List<Long> keyList = keys.list(20);

                System.out.println(keyList);
            }
        });
    }

    @Override
    public Identity doFind(final IdentityId userId) {
        Identity identity = dbi.withHandle(new HandleCallback<Identity>() {
            @Override
            public Identity withHandle(Handle handle) throws Exception {
                Map<String, Object> map = handle.createQuery("select * from users where " +
                        "userId=? and providerId=?")
                        .bind(0, userId.userId())
                        .bind(1, userId.providerId())
                        .first();

                System.out.println(map);

                deflatMap(map, "oAuth1Info");
                deflatMap(map, "oAuth2Info");

                Identity identity = MongoUserService.mapToIdentity(userId, map);

                return identity;
            }
        });

        System.out.println(identity);

        return users.get(userId.userId() + userId.providerId());
    }

    @Override
    public Token doFindToken(String tokenId) {
        return tokens.get(tokenId);
    }

    @Override
    public Identity doFindByEmailAndProvider(String email, String providerId) {
        Identity result = null;
        for (Identity user : users.values()) {
            Option<String> optionalEmail = user.email();
            if (user.identityId().providerId().equals(providerId) &&
                    optionalEmail.isDefined() &&
                    optionalEmail.get().equalsIgnoreCase(email)) {
                result = user;
                break;
            }
        }
        return result;
    }

    @Override
    public void doDeleteToken(String uuid) {
        tokens.remove(uuid);
    }

    @Override
    public void doDeleteExpiredTokens() {
        throw new UnsupportedOperationException("todo");
    }

    private Map<String, Object> deflatMap(Map<String, Object> map, String field) {
        Map<String, Object> temp = new HashMap<>();
        String fieldWithPrefix = field + "_";

        for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Object> e = it.next();
            if (!StringUtils.startsWithIgnoreCase(e.getKey(), fieldWithPrefix)) continue;

            it.remove();

            temp.put(e.getKey().substring(fieldWithPrefix.length()), e.getValue());
        }

        map.put(field, temp);

        return map;
    }

    //converts a multi-level map to a single level map
    private static Map<String, Object> flatMap(Map<String, Object> userMap) {
        List<Map<String, Object>> subObjects = new ArrayList<>();
        List<String> fields = new ArrayList<>();

        for (Iterator<Map.Entry<String, Object>> it = userMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Object> e = it.next();
            if (e.getValue() instanceof Map) {
                Map<String, Object> map = (Map) e.getValue();
                it.remove();
                subObjects.add(map);
                fields.add(e.getKey());
            }
        }

        int i = 0;
        for (Map<String, Object> map : subObjects) {
            String prefix = fields.get(i) + "_";
            for (Map.Entry<String, Object> e : map.entrySet()) {
                userMap.put(prefix + e.getKey(), e.getValue());
            }

            i++;
        }

        if (!subObjects.isEmpty()) {
            flatMap(userMap);
        }

        return userMap;
    }

    private static Update bindParams(Update update, Map<String, Object> userMap) {
        int i = 0;
        for (Object o : userMap.values()) {
            update.bind(i++, o);
        }

        return update;
    }

    private static StringBuilder buildValues(String operation, Map<String, Object> userMap) {
        StringBuilder sb = new StringBuilder(1024);

        sb.append(operation);
        sb.append("(");

        int index = 0, size = userMap.size();

        for (Map.Entry<String, Object> e : userMap.entrySet()) {
            sb.append(e.getKey());
            if (index != size - 1) {
                sb.append(", ");
            }
            index++;
        }

        sb.append(") ");
        sb.append(" values (");
        Joiner.on(", ").appendTo(sb, Collections.nCopies(size, '?'));
        sb.append(")");
        return sb;
    }
}
