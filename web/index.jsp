<%-- Created by IntelliJ IDEA. --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
</head>
<body>
gitlab commit reviewer project!
<br/>
interface document
<br/>
<h1></h1>
<table border="1">
    <caption>添加commit（此接口有判重功能，如果新添加的提交已经存在，则不会重复添加）</caption>
    <tr>
        <td>请求方式</td>
        <td colspan="2">get&post</td>
    </tr>
    <tr>
        <td>url</td>
        <td colspan="2">/githook</td>
    </tr>
    <tr>
        <td>请求参数</td>
        <td>cmd</td>
        <td>add</td>
    </tr>
    <tr>
        <td></td>
        <td>commits</td>
        <td>*a json array*</td>
    </tr>
    <tr>
        <td></td>
        <td>user</td>
        <td>提交者名称</td>
    </tr>
    <tr>
        <td></td>
        <td>versionHash</td>
        <td>提交hash值（唯一）</td>
    </tr>
    <tr>
        <td></td>
        <td>time</td>
        <td>提交时间（格式：yy-mm-dd HH:MM:SS）</td>
    </tr>
    <tr>
        <td></td>
        <td>msg</td>
        <td>提交信息</td>
    </tr>
    <tr>
        <td>响应参数</td>
        <td>msg</td>
        <td>success/fail</td>
    </tr>
    <tr>
        <td>示例</td>
        <td colspan="2">
            <a href="/githook?cmd=add&commits=[{'user':'提交者','versionHash':'957b16ee2adf8c562dda54010ddef4831536a6a1','time':'19-07-33
            14:31:59','msg':'abc'},{'user':'提交者','versionHash':'52837011285887299de164df6ec5556595e9ce11','time':'19-07-31
            14:31:24','msg':'abcd'}]">/githook?cmd=add&commits=[{"
                user":"提交者","versionHash":"957b16ee2adf8c562dda54010ddef4831536a6a1","time":"19-07-31
                14:31:59","msg":"abc"},{"user":"提交者","versionHash":"52837011285887299de164df6ec5556595e9ce11","time":"19-07-31
                14:31:24","msg":"abcd"}]
            </a>
        </td>
    </tr>
</table>

</body>
</html>