package cn.kuwo.outer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteHelper {
    private static String Drivde = "org.sqlite.JDBC";
    private final int version = 2;
    private static String TABLE_COMMIT_INFO = "commit_info";
    //这个数组只能顺序的朝后加！！！
    public final static String[] COMMIT_COLUMN_INFO = new String[]{
            "submitter",
            "version_hash",
            "commit_time",
            "commit_msg",
            "reviewer",
            "review_state",
            "review_time",
            "review_comment",
            "project_name"
    };
    private Connection connection;
    private static SqliteHelper sqliteHelper;


    private SqliteHelper() {
        try {
            updateDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static SqliteHelper getInstance() {
        if (sqliteHelper == null) {
            synchronized (SqliteHelper.class) {
                if (sqliteHelper == null) {
                    sqliteHelper = new SqliteHelper();
                }
            }
        }
        return sqliteHelper;
    }

    private Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName(Drivde);// 加载驱动,连接sqlite的jdbc
                connection = DriverManager.getConnection("jdbc:sqlite:gitCommits.db");
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    private void updateDatabase() throws SQLException {
        Connection connection = getConnection();
        if (connection != null) {
            Statement statement = connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
            ResultSet resultSet = statement.executeQuery("PRAGMA user_version");
            int oldVersion = resultSet.getInt("user_version");
            onUpdate(version, oldVersion);
            if (oldVersion < version) {
                String sql = "create table " + TABLE_COMMIT_INFO + "(" + COMMIT_COLUMN_INFO[8] + " varchar(20) ," + COMMIT_COLUMN_INFO[0] + " varchar(20)," + COMMIT_COLUMN_INFO[1] + " varchar(40)  PRIMARY KEY ON CONFLICT IGNORE," + COMMIT_COLUMN_INFO[2] + " varchar(20)," + COMMIT_COLUMN_INFO[3] + " varchar(400), " +
                        COMMIT_COLUMN_INFO[4] + " varchar(20)," + COMMIT_COLUMN_INFO[5] + " int ," + COMMIT_COLUMN_INFO[6] + " varchar(20) ," + COMMIT_COLUMN_INFO[7] + " varchar(400) )";
                statement.executeUpdate(sql);
            }
            statement.executeUpdate("PRAGMA user_version=" + version);
            statement.close();
        }
    }

    private void onUpdate(int version, int oldVersion) {

    }

    public boolean insertData(List<CommitInfo> commitInfos) {
        boolean result = false;
        Connection connection = getConnection();
        if (connection != null && commitInfos != null && commitInfos.size() > 0) {
            try {
                Statement statement = connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
                StringBuffer values = new StringBuffer();
                for (int i = 0; i < commitInfos.size(); i++) {
                    CommitInfo commitInfo = commitInfos.get(i);
                    checkString(commitInfo);
                    values.append(String.format("('%s','%s','%s','%s','%s','%s','%d','%s','%s')", commitInfo.project_name,
                            commitInfo.submitter, commitInfo.version_hash, commitInfo.commit_time, commitInfo.commit_msg, commitInfo.reviewer, commitInfo.review_state, commitInfo.review_time, commitInfo.review_comment));
                    if (i != commitInfos.size() - 1) {
                        values.append(",");
                    }
                }
                String sql = String.format("insert into %s values %s", TABLE_COMMIT_INFO, values.toString());
                int i = statement.executeUpdate(sql);//向数据库中插入数据
                result = i > 0;
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 引号会导致问题，所以编码
     *
     * @param commitInfo
     */
    private void checkString(CommitInfo commitInfo) {
        commitInfo.project_name = URLEncoder.encode(commitInfo.project_name);
        commitInfo.commit_msg = URLEncoder.encode(commitInfo.commit_msg);
        commitInfo.submitter = URLEncoder.encode(commitInfo.submitter);
        commitInfo.review_comment = URLEncoder.encode(commitInfo.review_comment);
        commitInfo.reviewer = URLEncoder.encode(commitInfo.reviewer);
    }

    public CommitInfo update(CommitInfo commitInfo) {
        Connection connection = getConnection();
        try {
            if (connection != null && !connection.isClosed() && commitInfo != null && commitInfo.version_hash != null) {
                Statement statement = connection.createStatement();
                checkString(commitInfo);
                StringBuffer stringBuffer = new StringBuffer();
                if (commitInfo.reviewer != null && !commitInfo.reviewer.isEmpty()) {
                    stringBuffer.append(COMMIT_COLUMN_INFO[4]);
                    stringBuffer.append("=");
                    stringBuffer.append("'");
                    stringBuffer.append(commitInfo.reviewer);
                    stringBuffer.append("'");
                    stringBuffer.append(",");
                }
                if (commitInfo.review_state == 1) {
                    stringBuffer.append(COMMIT_COLUMN_INFO[5]);
                    stringBuffer.append("=");
                    stringBuffer.append(commitInfo.review_state);
                    stringBuffer.append(",");
                }
                if (commitInfo.review_time != null && !commitInfo.reviewer.isEmpty()) {
                    stringBuffer.append(COMMIT_COLUMN_INFO[6]);
                    stringBuffer.append("=");
                    stringBuffer.append("'");
                    stringBuffer.append(commitInfo.review_time);
                    stringBuffer.append("'");
                    stringBuffer.append(",");
                }
                if (commitInfo.review_comment != null && !commitInfo.reviewer.isEmpty()) {
                    stringBuffer.append(COMMIT_COLUMN_INFO[7]);
                    stringBuffer.append("=");
                    stringBuffer.append("'");
                    stringBuffer.append(commitInfo.review_comment);
                    stringBuffer.append("'");
                    stringBuffer.append(",");
                }
                String s = stringBuffer.toString();
                if (s.endsWith(",")) {
                    s = s.substring(0, s.length() - 1);
                }
                if (s.isEmpty()) {
                    return null;
                }
                String where = COMMIT_COLUMN_INFO[1] + "='" + commitInfo.version_hash + "'";
                statement.executeUpdate("update " + TABLE_COMMIT_INFO + " set " + s + " where " + where);
                statement.close();
                ArrayList<CommitInfo> query = query(where);
                if (query != null && query.size() > 0) {
                    return query.get(0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<CommitInfo> query(String where) {
        Connection connection = getConnection();
        ArrayList<CommitInfo> commitInfos = null;
        try {
            if (connection != null && !connection.isClosed()) {
                Statement statement = connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
                String limit = where == null || where.isEmpty() ? "" : " where " + where;
                ResultSet resultSet = statement.executeQuery("select * from " + TABLE_COMMIT_INFO + limit);//搜索数据库，将搜索的放入数据集ResultSet中
                commitInfos = new ArrayList<>(resultSet.getFetchSize());
                while (resultSet.next()) {
                    CommitInfo commitInfo = new CommitInfo();
                    commitInfo.submitter = resultSet.getString(COMMIT_COLUMN_INFO[0]);
                    commitInfo.version_hash = resultSet.getString(COMMIT_COLUMN_INFO[1]);
                    commitInfo.commit_time = resultSet.getString(COMMIT_COLUMN_INFO[2]);
                    commitInfo.commit_msg = resultSet.getString(COMMIT_COLUMN_INFO[3]);
                    commitInfo.reviewer = resultSet.getString(COMMIT_COLUMN_INFO[4]);
                    commitInfo.review_state = resultSet.getInt(COMMIT_COLUMN_INFO[5]);
                    commitInfo.review_time = resultSet.getString(COMMIT_COLUMN_INFO[6]);
                    commitInfo.review_comment = resultSet.getString(COMMIT_COLUMN_INFO[7]);
                    commitInfo.project_name = resultSet.getString(COMMIT_COLUMN_INFO[8]);
                    commitInfos.add(commitInfo);
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commitInfos;
    }
}
