package cn.kuwo.outer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "GitHookServlet")
public class GitHookServlet extends HttpServlet {
    private final static String CMD_ADD = "add";
    private final static String CMD_UPDATE = "update";
    private final static String CMD_QUERY = "query";
    private Type gsonTypeList;

    @Override
    public void init() throws ServletException {
        super.init();
        gsonTypeList = new TypeToken<List<CommitInfo>>() {
        }.getType();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handRequest(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handRequest(request, response);
    }

    private void handRequest(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        String cmd = request.getParameter("cmd");

        if (cmd != null) {
            switch (cmd.toLowerCase()) {
                case CMD_ADD:
                    boolean b = addCommits(request);
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("msg", (b ? "success" : "fail"));
                    jsonObject.addProperty("code", b ? 0 : -1);
                    String msg = jsonObject.toString();
                    try {
                        response.getOutputStream().write(msg.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CMD_QUERY:
                    try {
                        String query = query(request);
                        response.getOutputStream().write(query.getBytes("utf-8"));
                        response.getOutputStream().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CMD_UPDATE:
                    String update = update(request);
                    try {
                        ServletOutputStream outputStream = response.getOutputStream();
                        outputStream.write(update.getBytes());
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String update(HttpServletRequest request) {
        CommitInfo commitInfo = new CommitInfo();
        commitInfo.version_hash = request.getParameter("versionHash");
        commitInfo.review_time = simpleDateFormat.format(new Date());
        commitInfo.reviewer = request.getParameter("reviewer");
        try {
            commitInfo.review_comment = new String(request.getParameter("review_comment").getBytes("iso-8859-1"), "utf-8");
            commitInfo.reviewer = new String(request.getParameter("reviewer").getBytes("iso-8859-1"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("code", -1);
            jsonObject.addProperty("msg", "charset err");
            return jsonObject.toString();
        }
        if (commitInfo.reviewer == null || commitInfo.reviewer.isEmpty()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("code", -1);
            jsonObject.addProperty("msg", "reviewer is null");
            return jsonObject.toString();
        }
        String review_state = request.getParameter("review_state");
        if (review_state != null) {
            try {
                commitInfo.review_state = Integer.parseInt(review_state) > 0 ? 1 : 0;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("code", -1);
                jsonObject.addProperty("msg", "review_state error");
                return jsonObject.toString();
            }
        }
        SqliteHelper instance = SqliteHelper.getInstance();
        CommitInfo update = instance.update(commitInfo);
        JsonObject jsonObject = new JsonObject();
        if (update != null) {
            jsonObject.addProperty("code", 0);
            jsonObject.addProperty("data", new Gson().toJson(update, new TypeToken<CommitInfo>() {
            }.getType()));
            jsonObject.addProperty("msg", "success");
        } else {
            jsonObject.addProperty("code", -1);
            jsonObject.addProperty("msg", "fail");
        }
        return jsonObject.toString();
    }

    private String query(HttpServletRequest request) {
        SqliteHelper instance = SqliteHelper.getInstance();
        StringBuffer stringBuffer = new StringBuffer();
        String project_names = request.getParameter("project_names");
        if (project_names != null && !project_names.isEmpty()) {
            String[] split = project_names.split(",");
            String project_name = null;
            for (int i = 0; i < split.length; i++) {
                project_name = split[i];
                stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[8]);
                stringBuffer.append("=");
                stringBuffer.append("'");
                stringBuffer.append(URLEncoder.encode(project_name));
                if (i == split.length - 1) {
                    stringBuffer.append("'");
                } else {
                    stringBuffer.append("' or ");
                }
            }
            stringBuffer.append(" and ");
        }
        String commit_start_time = request.getParameter("commit_start_time");
        if (commit_start_time != null && !commit_start_time.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[2]);
            stringBuffer.append(">=");
            stringBuffer.append("'");
            stringBuffer.append(commit_start_time);
            stringBuffer.append("'");
            stringBuffer.append(" and ");
        }
        String commit_end_time = request.getParameter("commit_end_time");
        if (commit_end_time != null && !commit_end_time.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[2]);
            stringBuffer.append("<=");
            stringBuffer.append("'");
            stringBuffer.append(commit_end_time);
            stringBuffer.append("'");
            stringBuffer.append(" and ");
        }
        String user = request.getParameter("user");
        if (user != null && !user.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[1]);
            stringBuffer.append("=");
            stringBuffer.append("'");
            stringBuffer.append(URLEncoder.encode(user));
            stringBuffer.append("'");
            stringBuffer.append(" and ");
        }
        String review_state = request.getParameter("review_state");
        if (review_state != null && !review_state.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[5]);
            stringBuffer.append("=");
            stringBuffer.append(review_state);
            stringBuffer.append(" and ");
        }
        String reviewer = request.getParameter("reviewer");
        if (reviewer != null && !reviewer.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[4]);
            stringBuffer.append("=");
            stringBuffer.append("'");
            stringBuffer.append(URLEncoder.encode(reviewer));
            stringBuffer.append("'");
            stringBuffer.append(" and ");
        }
        String review_start_time = request.getParameter("review_start_time");
        if (review_start_time != null && !review_start_time.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[6]);
            stringBuffer.append(">=");
            stringBuffer.append("'");
            stringBuffer.append(review_start_time);
            stringBuffer.append("'");
            stringBuffer.append(" and ");
        }
        String review_end_time = request.getParameter("review_end_time");
        if (review_end_time != null && !review_end_time.isEmpty()) {
            stringBuffer.append(SqliteHelper.COMMIT_COLUMN_INFO[6]);
            stringBuffer.append("<=");
            stringBuffer.append("'");
            stringBuffer.append(review_end_time);
            stringBuffer.append("'");
            stringBuffer.append(" and ");
        }
        String where = stringBuffer.toString();
        if (where.endsWith(" and ")) {
            where = where.substring(0, where.length() - 5);
        }
        String requestCount = request.getParameter("requestCount");
        if (isInteger(requestCount)) {
            int count = Integer.parseInt(requestCount);
            if (count > 0) {
                if (count > 0) {
                    where = where + " order by rowid desc limit " + count;
                }
            }
        }
        ArrayList<CommitInfo> query = instance.query(where);
        Gson gson = new Gson();
        NetData netData = new NetData();
        netData.code = 0;
        netData.msg = "success";
        netData.data = query;
        return gson.toJson(netData);
    }

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private boolean addCommits(HttpServletRequest request) throws UnsupportedEncodingException {
        String commits = request.getParameter("commits");
        if (commits == null || commits.isEmpty()) {
            return false;
        }
        System.out.println("commits==" + commits);
        commits = new String(commits.getBytes("iso-8859-1"), "utf-8");
        System.out.println("commits==" + commits);
        //处理下 msg 字段中的双引号
        String patternStr = "(?<=,\"msg\":\").*(?=\"\\})";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(commits);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group();
            String encode = URLEncoder.encode(group);
            matcher.appendReplacement(stringBuffer, encode);
        }
        matcher.appendTail(stringBuffer);
        commits = stringBuffer.toString();

        Gson gson = new Gson();
        ArrayList<CommitInfo> list = gson.fromJson(commits, gsonTypeList);
        SqliteHelper instance = SqliteHelper.getInstance();
        return instance.insertData(list);
    }

    public class NetData {
        public int code;
        public Object data;
        public String msg;
    }
}
