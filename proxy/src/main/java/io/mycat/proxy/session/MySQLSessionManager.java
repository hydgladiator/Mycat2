///**
// * Copyright (C) <2021>  <chen junwen>
// * <p>
// * This program is free software: you can redistribute it and/or modify it under the terms of the
// * GNU General Public License as published by the Free Software Foundation, either version 3 of the
// * License, or (at your option) any later version.
// * <p>
// * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * General Public License for more details.
// * <p>
// * You should have received a copy of the GNU General Public License along with this program.  If
// * not, see <http://www.gnu.org/licenses/>.
// */
//package io.mycat.proxy.session;
//
//import io.mycat.GlobalConst;
//import io.mycat.MycatException;
//import io.mycat.api.collector.OneResultSetCollector;
//import io.mycat.api.collector.TextResultSetTransforCollector;
//import io.mycat.beans.MySQLDatasource;
//import io.mycat.beans.mysql.MySQLPayloadWriter;
//import io.mycat.beans.mysql.packet.ErrorPacketImpl;
//import io.mycat.proxy.callback.CommandCallBack;
//import io.mycat.proxy.callback.RequestCallback;
//import io.mycat.proxy.callback.ResultSetCallBack;
//import io.mycat.proxy.callback.SessionCallBack;
//import io.mycat.proxy.handler.backend.*;
//import io.mycat.proxy.monitor.MycatMonitor;
//import io.mycat.proxy.reactor.MycatReactorThread;
//import io.mycat.proxy.reactor.NIOJob;
//import io.mycat.proxy.reactor.ReactorEnvThread;
//import io.mycat.proxy.session.SessionManager.BackendSessionManager;
//import io.mycat.util.StringUtil;
//import io.mycat.util.nio.NIOUtil;
//import io.vertx.core.CompositeFuture;
//import io.vertx.core.Future;
//import io.vertx.core.Promise;
//import io.vertx.core.impl.future.PromiseInternal;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.stream.Collectors;
//
//import static io.mycat.beans.mysql.MySQLCommandType.COM_QUERY;
//
///**
// * ????????????MySQL LocalInFileSession ??????mycat proxy???,??????????????????mysql session????????????mysqlsession?????????
// * ???????????????????????????,????????????????????????????????????
// *
// * @author jamie12221 date 2019-05-10 13:21
// **/
//public class MySQLSessionManager implements
//        BackendSessionManager<MySQLClientSession, MySQLDatasource> {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLSessionManager.class);
//    final ConcurrentHashMap<Long, MySQLClientSession> allSessions = new ConcurrentHashMap<>();
//    final ConcurrentHashMap<String, LinkedList<MySQLClientSession>> idleDatasourcehMap = new ConcurrentHashMap<>();
//
////  private ProxyRuntime runtime;
//
//    public MySQLSessionManager() {
//    }
//
//    /**
//     * ?????????????????????,??????????????????????????????allSessions????????????MySQLSession
//     */
//    @Override
//    public final List<MySQLClientSession> getAllSessions() {
//        return new ArrayList<>(allSessions.values());
//    }
//
//    /**
//     * ??????mysql proxy???,?????????????????????mysqlSession?????????
//     */
//    @Override
//    public final int currentSessionCount() {
//        return allSessions.size();
//    }
//
//
//    @Override
//    public void getIdleSessionsOfIdsOrPartial(MySQLDatasource datasource, List<SessionIdAble> ids,
//                                              PartialType partialType,
//                                              SessionCallBack<MySQLClientSession> arg) {
//        Objects.requireNonNull(datasource);
//        SessionCallBack<MySQLClientSession> asyncTaskCallBack = new SessionCallBack<MySQLClientSession>() {
//            @Override
//            public void onSession(MySQLClientSession session, Object sender, Object attr) {
////                datasource.tryIncrementUsedCounter();//???????????????????????????
//                arg.onSession(session, sender, attr);
//            }
//
//            @Override
//            public void onException(Exception exception, Object sender, Object attr) {
//                arg.onException(exception, sender, attr);
//            }
//        };
//        try {
//            for (; ; ) {//?????????????????????return
//                MySQLClientSession mySQLSession = getIdleMySQLClientSessionsByIds(datasource, ids, partialType);
//                if (mySQLSession == null) {
//                    createSession(datasource, asyncTaskCallBack);
//                    return;
//                }
//                if (!mySQLSession.checkOpen()) {
//                    continue;
//                }
//                assert mySQLSession.getCurNIOHandler() == IdleHandler.INSTANCE;
//                assert mySQLSession.currentProxyBuffer() == null;
//                mySQLSession.setIdle(false);
//                mySQLSession.switchNioHandler(null);
//                MycatMonitor.onGetIdleMysqlSession(mySQLSession);
//                asyncTaskCallBack.onSession(mySQLSession, this, null);
//                return;
//            }
//        } catch (Exception e) {
//            LOGGER.error("", e);
//            asyncTaskCallBack
//                    .onException(e, this,
//                            null);
//        }
//    }
//
//    /**
//     * @param ids ??????id?????? ?????????-id
//     */
//    private MySQLClientSession getIdleMySQLClientSessionsByIds(MySQLDatasource datasource,
//                                                              List<SessionIdAble> ids, PartialType partialType) {
//        MySQLClientSession session = null;
//        //dataSource
//        if (datasource != null && (ids == null || ids.isEmpty())) {
//            LinkedList<MySQLClientSession> group = this.idleDatasourcehMap.get(datasource.getName());
//            for (; ; ) {
//                if (group == null || group.isEmpty()) {
//                    return null;
//                }
//
//                if (partialType == PartialType.RANDOM_ID || partialType == null) {
//                    boolean random = ThreadLocalRandom.current().nextBoolean();
//                    session = random ? group.removeFirst() : group.removeLast();
//                } else {
//                    group.sort(Comparator.comparing(AbstractSession::sessionId));
//                    switch (partialType) {
//                        case SMALL_ID:
//                            session = group.removeFirst();
//                            break;
//                        case LARGE_ID:
//                            session = group.removeLast();
//                            break;
//                    }
//                }
//                return session;
//            }
//        }
//        //dataSource ids
//        else if (datasource != null && ids != null) {
//            session = searchMap(ids, this.allSessions);
//        }
//        //ids
//        else if (ids != null && ids.size() > 0) {
//            session = searchMap(ids, this.allSessions);
//        }
//        return session;
//    }
//
//
//    private MySQLClientSession searchMap(List<SessionIdAble> ids,
//                                         Map<Long, MySQLClientSession> source) {
//        int size = ids.size();
//        for (int i = 0; i < size; i++) {
//            long id = ids.get(i).getSessionId();
//            MySQLClientSession mySQLClientSession = source.get(id);
//            if (mySQLClientSession.isIdle()) {
//                LinkedList<MySQLClientSession> sessions = this.idleDatasourcehMap
//                        .get(mySQLClientSession.getDatasource().getName());
//                sessions.remove(mySQLClientSession);
//                return mySQLClientSession;
//            }
//        }
//        return null;
//    }
//
//
//    /**
//     * ??????dataSource??????????????????????????????MySQLSession 0.??????dataSource????????????,?????????????????????,???????????????callback???attr
//     * 1.??????????????????session???????????????????????? ??????????????????session????????????????????????????????????session 2.??????????????????session
//     */
//
//    @Override
//    public final void getIdleSessionsOfKey(MySQLDatasource datasource,
//                                           SessionCallBack<MySQLClientSession> asyncTaskCallBack) {
//        getIdleSessionsOfIdsOrPartial(datasource, null, PartialType.RANDOM_ID, asyncTaskCallBack);
//    }
//
//    /**
//     * ??????????????????mysql session?????????????????? 1.?????????session???????????????????????????,????????????????????????,?????????????????????
//     * 2.????????????????????????,session????????????????????????,????????????????????????,??????????????????????????????
//     */
//    @Override
//    public final void addIdleSession(MySQLClientSession session) {
//        try {
//            /**
//             * mycat?????????????????????mysql session?????????
//             * niohandler??????????????????task??????mysql session?????????
//             */
//            assert session.getMycat() == null;
//            assert !session.hasClosed();
//            assert session.currentProxyBuffer() == null;
//            assert !session.isIdle();
//            /////////////////////////////////////////
//
////            session.getDatasource().decrementUsedCounter();
//            ////////////////////////////////////////
//            //////////////////////////////////////////////////
//            session.setCursorStatementId(-1);
//            session.resetPacket();
//            session.setIdle(true);
//            session.switchNioHandler(IdleHandler.INSTANCE);
//            session.change2ReadOpts();
//            LinkedList<MySQLClientSession> idleList = idleDatasourcehMap.computeIfAbsent(session.getDatasource().getName(), (l) -> new LinkedList<>());
//            idleList.add(session);
//            MycatMonitor.onAddIdleMysqlSession(session);
//        } catch (Exception e) {
//            LOGGER.error("", e);
//            session.close(false, e);
//        }
//    }
//
//    /**
//     * 1.????????????????????????mysql session 2.?????????????????????session 3.??????????????????????????????,??????????????????????????????
//     */
//    private void removeIdleSession(MySQLClientSession session) {
//        try {
//            assert session != null;
//            assert session.getDatasource() != null;
//            LinkedList<MySQLClientSession> mySQLSessions = idleDatasourcehMap
//                    .get(session.getDatasource().getName());
//            if (mySQLSessions != null) {
//                mySQLSessions.remove(session);
//            }
//        } catch (Exception e) {
//            LOGGER.error("", e);
//        }
//    }
//
//    /**
//     * 1.??????????????????????????? a.??????????????????dataSource????????? b.??????????????????dataSource?????????
//     *//**/
//    @Override
//    public final void clearAndDestroyDataSource(MySQLDatasource key, String reason) {
//        assert key != null;
//        assert reason != null;
//        Collection<MySQLClientSession> allSessions = Collections
//                .unmodifiableCollection(this.allSessions.values());
//        for (MySQLClientSession s : allSessions) {
//            if (s.getDatasource().getName().equals(key.getName())) {
//                this.allSessions.remove(s.sessionId());
//            }
//        }
//        LinkedList<MySQLClientSession> sessions = idleDatasourcehMap.get(key.getName());
//        if (sessions != null) {
//            for (MySQLClientSession session : sessions) {
//                try {
//                    session.close(true, reason);
//                } catch (Exception e) {
//                    LOGGER.error("mysql session is closing but occur error", e);
//                }
//            }
//        }
//        idleDatasourcehMap.remove(key.getName());
//    }
//
//    /*
//      1.??????????????????????????????
//      2.???????????????????????????
//      3.???????????????????????????, ?????????????????????
//      4.???????????? ????????????????????????
//     */
//    @Override
//    public void idleConnectCheck() {
//        MycatReactorThread thread = (MycatReactorThread) Thread.currentThread();
//
//        idleDatasourcehMap.forEach((name, v) -> {
//            //??????DataSourceName??????DataSource,??????MySQLClientSession????????????
//            MySQLClientSession session = Optional.ofNullable(idleDatasourcehMap.get(name))
//                    .filter(i->!i.isEmpty())
//                    .map(i -> i.getFirst()).orElse(null);
//            if (session == null) {
//                return;
//            }
//            MySQLDatasource datasource  = session.getDatasource();
//
//            long idleTimeout = datasource.getIdleTimeout();
//            long hearBeatTime = System.currentTimeMillis() - idleTimeout;
//            long hearBeatTime2 = System.currentTimeMillis() - 2 * idleTimeout;
//            int maxConsInOneCheck = Math.min(10, datasource.getSessionMinCount());//??????????????????10?????????????????????
//            LinkedList<MySQLClientSession> group = idleDatasourcehMap.get(name);
//            List<MySQLClientSession> checkList = new ArrayList<>();
//            //????????????
//            if (null != group) {
//                checkIfNeedHeartBeat(hearBeatTime, hearBeatTime2, maxConsInOneCheck, group, checkList);
//                for (MySQLClientSession mySQLClientSession : checkList) {
//                    sendPing(mySQLClientSession);
//                }
//            }
//            int idleCount = group == null ? 0 : group.size();
//            int createCount =Math.max(0,datasource.getSessionMinCount()-datasource.getConnectionCounter());
//            if (createCount > 0) {
//                createByLittle(datasource, createCount);
//            } else if (idleCount - checkList.size() > datasource.getSessionMinCount()
//                    && group != null) {
//                //??????????????????
//                closeByMany(datasource,
//                        idleCount - checkList.size() - datasource.getSessionMinCount());
//            }
//
//        });
//
//    }
//
//    public Future<Void> reset(){
//        idleDatasourcehMap.clear();
//        return (Future)CompositeFuture.join(allSessions.entrySet().stream()
//                .map(c -> c.getValue().close(true, "pool close").future())
//                .collect(Collectors.toList()));
//    }
//
//    private void closeByMany(MySQLDatasource mySQLDatasource, int closeCount) {
//        LinkedList<MySQLClientSession> group = this.idleDatasourcehMap.get(mySQLDatasource.getName());
//
//        for (int i = 0; i < closeCount; i++) {
//            MySQLClientSession mySQLClientSession = group.removeFirst();
//            if (mySQLClientSession != null) {
//                closeSession(mySQLClientSession, "mysql session  close because of idle");
//            }
//        }
//    }
//
//    private void createByLittle(MySQLDatasource mySQLDatasource, int createCount) {
//        for (int i = 0; i < createCount; i++) {
//            //????????????
//            createSession(mySQLDatasource, new SessionCallBack<MySQLClientSession>() {
//                @Override
//                public void onSession(MySQLClientSession session, Object sender, Object attr) {
//                    session.getSessionManager().addIdleSession(session);
//                }
//
//                @Override
//                public void onException(Exception exception, Object sender, Object attr) {
//                    //????????????
//                    MycatMonitor.onBackendConCreateException(null, exception);
//                }
//            });
//        }
//    }
//
//    private void sendPing(MySQLClientSession session) {
//        OneResultSetCollector collector = new OneResultSetCollector();
//        TextResultSetTransforCollector transfor = new TextResultSetTransforCollector(collector);
//        TextResultSetHandler queryResultSetTask = new TextResultSetHandler(transfor);
//        queryResultSetTask
//                .request(session, COM_QUERY, GlobalConst.SINGLE_NODE_HEARTBEAT_SQL,
//                        new ResultSetCallBack<MySQLClientSession>() {
//                            @Override
//                            public void onFinishedSendException(Exception exception, Object sender,
//                                                                Object attr) {
//                                closeSession(session, "send Ping error");
//                            }
//
//                            @Override
//                            public void onFinishedException(Exception exception, Object sender, Object attr) {
//                                closeSession(session, "send Ping error");
//                            }
//
//                            @Override
//                            public void onFinished(boolean monopolize, MySQLClientSession mysql, Object sender,
//                                                   Object attr) {
//                                mysql.getSessionManager().addIdleSession(mysql);
//                            }
//
//                            @Override
//                            public void onErrorPacket(ErrorPacketImpl errorPacket, boolean monopolize,
//                                                      MySQLClientSession mysql, Object sender, Object attr) {
//                                closeSession(session, "send Ping error ");
//                            }
//                        });
//    }
//
//    private void checkIfNeedHeartBeat(long hearBeatTime, long hearBeatTime2, int maxConsInOneCheck,
//                                      LinkedList<MySQLClientSession> group, List<MySQLClientSession> checkList) {
//        Iterator<MySQLClientSession> iterator = group.iterator();
//        while (iterator.hasNext()) {
//            MySQLClientSession mySQLClientSession = iterator.next();
//            if (!mySQLClientSession.getDatasource().isValid()){
//                closeSession(mySQLClientSession,"not valid");
//                continue;
//            }
//            //??????
//            if (!mySQLClientSession.checkOpen()) {
//                closeSession(mySQLClientSession, "mysql session  close because of idle");
//                iterator.remove();
//                continue;
//            }
//            long lastActiveTime = mySQLClientSession.getLastActiveTime();
//            if (lastActiveTime < hearBeatTime
//                    && checkList.size() < maxConsInOneCheck) {
//                mySQLClientSession.setIdle(false);
//                checkList.add(mySQLClientSession); //??????ping??????
//                MycatMonitor.onGetIdleMysqlSession(mySQLClientSession);
//                iterator.remove();
//
//            } else if (lastActiveTime < hearBeatTime2) {
//                closeSession(mySQLClientSession, "mysql session is close in idle");
//                iterator.remove();
//            }
//        }
//    }
//
//    private void closeSession(MySQLClientSession mySQLClientSession, String hint) {
//        mySQLClientSession.setIdle(false);
//        MycatReactorThread mycatReactorThread = mySQLClientSession.getIOThread();
//        mycatReactorThread.addNIOJob(new NIOJob() {
//            @Override
//            public void run(ReactorEnvThread reactor) throws Exception {
//                mySQLClientSession.close(false, hint);
//            }
//
//            @Override
//            public void stop(ReactorEnvThread reactor, Exception reason) {
//                mySQLClientSession.close(false, hint);
//            }
//
//            @Override
//            public String message() {
//                return hint;
//            }
//        });
//    }
//
//    final static Exception SESSION_MAX_COUNT_LIMIT = new Exception("session max count limit");
//
//    /**
//     * ??????dataSource????????????MySQL LocalInFileSession 1.?????????????????????????????????????????????????????????,???????????????session?????????????????????,??????????????????????????????????????????????????????????????????
//     * 2.?????????????????????attr
//     */
//    @Override
//    public void createSession(MySQLDatasource key, SessionCallBack<MySQLClientSession> callBack) {
//        assert key != null;
//        assert callBack != null;
//        if (!key.tryIncrementSessionCounter()) {
//            callBack.onException(SESSION_MAX_COUNT_LIMIT, this, null);
//            return;
//        }
//        int maxRetry = key.gerMaxRetry();
//        createCon(key, new SessionCallBack<MySQLClientSession>() {
//            int retryCount = 0;
//            final long startTime = System.currentTimeMillis();
//
//            @Override
//            public void onSession(MySQLClientSession session, Object sender, Object attr) {
//                callBack.onSession(session, sender, attr);
//            }
//
//            @Override
//            public void onException(Exception exception, Object sender, Object attr) {
//                long now = System.currentTimeMillis();
//                long maxConnectTimeout = key.getMaxConnectTimeout();
//                if (retryCount >= maxRetry || startTime + maxConnectTimeout > now) { // retryCount==maxRetry??????????????????????????????
//                    callBack.onException(exception, sender, attr);
//                } else {
//                    ++retryCount;
//
//                    int retryInterval = (maxRetry - retryCount == 0) ? 1 : maxRetry - retryCount; // ??????1????????????????????????
//                    long waitTime = (maxConnectTimeout + startTime - now) / retryInterval; //????????????/????????????=??????????????????
//
//                    // long waitTime = Math.min(0,maxConnectTimeout + startTime - now) / Math.min(1,maxRetry - retryCount);//???????????????????????????????????????????????????
//
//                    MycatReactorThread thread = (MycatReactorThread) Thread.currentThread();
//                    SessionCallBack<MySQLClientSession> sessionCallBack = this;
//                    Runnable runnable = (() -> thread.addNIOJob(new NIOJob() {
//                        @Override
//                        public void run(ReactorEnvThread reactor) throws Exception {
//                            createCon(key, sessionCallBack);
//                        }
//
//                        @Override
//                        public void stop(ReactorEnvThread reactor, Exception reason) {
//                            callBack.onException(reason, sender, attr);
//                        }
//
//                        @Override
//                        public String message() {
//                            return "waitTime:" + waitTime;
//                        }
//                    }));
//                    runnable.run();
//                }
//            }
//        });
//    }
//
//    private void createCon(MySQLDatasource key,
//                           SessionCallBack<MySQLClientSession> callBack) {
//        new BackendConCreateHandler(key, this,
//                (MycatReactorThread) Thread.currentThread(), new CommandCallBack() {
//            @Override
//            public void onFinishedOk(int serverStatus, MySQLClientSession session, Object sender,
//                                     Object attr) {
//                assert session.currentProxyBuffer() == null;
//                MycatMonitor.onNewMySQLSession(session);
//                MySQLDatasource datasource = session.getDatasource();
//                String sql = datasource.getInitSqlForProxy();
//                allSessions.put(session.sessionId(), session);
//                if (!StringUtil.isEmpty(sql)) {
//                    executeInitSQL(session, sql);
//                } else {
//                    callBack.onSession(session, sender, attr);
//                }
//            }
//
//            public void executeInitSQL(MySQLClientSession session, String sql) {
//                ResultSetHandler.DEFAULT.request(session, COM_QUERY,
//                        sql.getBytes(),
//                        new ResultSetCallBack<MySQLClientSession>() {
//                            @Override
//                            public void onFinishedSendException(Exception exception, Object sender,
//                                                                Object attr) {
//                                LOGGER.error("", exception);
//                                callBack.onException(exception, sender, attr);
//                            }
//
//                            @Override
//                            public void onFinishedException(Exception exception, Object sender, Object attr) {
//                                LOGGER.error("", exception);
//                                callBack.onException(exception, sender, attr);
//                            }
//
//                            @Override
//                            public void onFinished(boolean monopolize, MySQLClientSession mysql, Object sender,
//                                                   Object attr) {
//                                if (monopolize) {
//                                    String message = "mysql session is monopolized";
//                                    mysql.close(false, message);
//                                    callBack.onException(new MycatException(message), this, attr);
//                                } else {
//                                    callBack.onSession(mysql, this, attr);
//                                }
//                            }
//
//                            @Override
//                            public void onErrorPacket(ErrorPacketImpl errorPacket, boolean monopolize,
//                                                      MySQLClientSession mysql, Object sender, Object attr) {
//                                String message = errorPacket.getErrorMessageString();
//                                LOGGER.error(message);
//                                mysql.close(false, message);
//                                callBack.onException(new MycatException(message), sender, attr);
//                            }
//                        });
//            }
//
//
//            @Override
//            public void onFinishedException(Exception exception, Object sender, Object attr) {
//                key.decrementSessionCounter();
//                callBack.onException(exception, sender, attr);
//            }
//
//            @Override
//            public void onFinishedErrorPacket(ErrorPacketImpl errorPacket, int lastServerStatus,
//                                              MySQLClientSession session, Object sender, Object attr) {
//                key.decrementSessionCounter();
//                callBack.onException(toExpection(errorPacket), sender, attr);
//            }
//        });
//    }
//
//    /**
//     * ??????????????????session 1.???????????? 2.???????????? 3.???????????????????????????
//     */
//    @Override
//    public void removeSession(MySQLClientSession session, boolean normal, String reason) {
//        try {
//            assert session != null;
//            assert reason != null;
//            session.getDatasource().decrementSessionCounter();
//
//            allSessions.remove(session.sessionId());
//            MycatMonitor.onCloseMysqlSession(session, normal, reason);
//            removeIdleSession(session);
//            NIOUtil.close(session.channel());
//        } catch (Exception e) {
//            LOGGER.error("", e);
//        }
//    }
//
//
//}
