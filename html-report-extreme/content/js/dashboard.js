/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 84.41573033707866, "KoPercent": 15.584269662921349};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.7836516853932585, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "登出清理-1"], "isController": false}, {"data": [1.0, 500, 1500, "登出清理-0"], "isController": false}, {"data": [0.846, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.5, 500, 1500, "准备并发测试数据"], "isController": false}, {"data": [0.849, 500, 1500, "注册测试用户"], "isController": false}, {"data": [0.963, 500, 1500, "GET /api/tasks/{id}"], "isController": false}, {"data": [0.165, 500, 1500, "并发创建预约"], "isController": false}, {"data": [0.96, 500, 1500, "GET /api/tasks/{id}/slots"], "isController": false}, {"data": [0.819, 500, 1500, "POST /api/auth/register"], "isController": false}, {"data": [0.991, 500, 1500, "准备预约测试数据"], "isController": false}, {"data": [0.8858333333333334, 500, 1500, "注册用户"], "isController": false}, {"data": [0.974, 500, 1500, "查询任务"], "isController": false}, {"data": [0.8975, 500, 1500, "登录"], "isController": false}, {"data": [0.975, 500, 1500, "查询可用时段"], "isController": false}, {"data": [0.96, 500, 1500, "GET /api/tasks/{id}/slots/available"], "isController": false}, {"data": [0.991, 500, 1500, "准备登录测试用户"], "isController": false}, {"data": [0.991, 500, 1500, "生成注册数据"], "isController": false}, {"data": [0.357, 500, 1500, "POST /api/bookings (创建预约)"], "isController": false}, {"data": [0.0, 500, 1500, "登出清理"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 8900, 1387, 15.584269662921349, 219.8459550561793, 0, 1838, 160.0, 537.0, 635.0, 908.9499999999989, 472.09845109272226, 261.550446819303, 113.2365970785593], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["登出清理-1", 500, 500, 100.0, 2.1640000000000006, 1, 25, 2.0, 4.0, 5.0, 7.990000000000009, 32.478077297823965, 13.701688860019487, 5.899338259175057], "isController": false}, {"data": ["登出清理-0", 500, 0, 0.0, 3.0580000000000007, 0, 20, 2.0, 5.0, 8.0, 15.990000000000009, 32.45067497403946, 18.719349323403428, 8.505626135773625], "isController": false}, {"data": ["POST /api/auth/login", 500, 0, 0.0, 444.0359999999996, 95, 1498, 425.0, 632.8000000000001, 790.0499999999997, 1156.2100000000007, 32.14193880174852, 22.99529723257907, 9.002253953458473], "isController": false}, {"data": ["准备并发测试数据", 100, 0, 0.0, 718.8799999999999, 693, 790, 713.0, 747.0, 785.5999999999999, 790.0, 126.42225031605561, 3.580317635903919, 0.0], "isController": false}, {"data": ["注册测试用户", 500, 0, 0.0, 438.4739999999997, 103, 1396, 431.0, 614.8000000000001, 667.8499999999999, 1082.7400000000002, 32.16882197773918, 20.30707151129126, 13.351317715370264], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 0, 0.0, 260.314, 6, 972, 285.0, 482.0, 516.9, 710.8500000000001, 30.09872381411028, 23.279481699975925, 5.437757720322659], "isController": false}, {"data": ["并发创建预约", 100, 79, 79.0, 437.9700000000001, 211, 828, 446.5, 568.9, 582.9, 827.3299999999997, 118.90606420927467, 75.56224918252082, 38.551575505350776], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 0, 0.0, 266.68799999999993, 4, 982, 290.5, 494.90000000000003, 525.95, 927.5700000000004, 30.299357653617744, 20.191681311356202, 5.651540343594716], "isController": false}, {"data": ["POST /api/auth/register", 500, 0, 0.0, 463.68000000000023, 98, 1259, 445.0, 668.0, 771.95, 1084.93, 37.5911585595068, 23.694911331854748, 13.384508213668145], "isController": false}, {"data": ["准备预约测试数据", 500, 0, 0.0, 17.038000000000004, 0, 1728, 0.0, 1.0, 1.0, 934.6800000000021, 26.828352202607714, 0.6811886301443365, 0.0], "isController": false}, {"data": ["注册用户", 600, 0, 0.0, 375.8166666666666, 80, 1329, 349.0, 644.3999999999999, 722.9499999999999, 982.97, 35.26922172584059, 22.218002365976957, 14.252347607571126], "isController": false}, {"data": ["查询任务", 500, 0, 0.0, 211.3820000000005, 7, 983, 194.0, 451.0, 502.0, 747.7900000000002, 29.900729577801698, 23.126345532831, 7.329182738308814], "isController": false}, {"data": ["登录", 600, 0, 0.0, 350.0716666666667, 73, 1103, 326.5, 595.0, 695.8999999999999, 940.9100000000001, 35.40449637103912, 22.63951584351213, 11.604999225526642], "isController": false}, {"data": ["查询可用时段", 500, 0, 0.0, 207.5699999999999, 5, 1394, 186.0, 445.80000000000007, 501.69999999999993, 763.94, 29.91504128275697, 17.971636334061266, 7.800113303218859], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 0, 0.0, 252.11599999999984, 4, 1089, 268.5, 492.90000000000003, 533.9, 838.98, 30.18230109863576, 19.391362108384644, 5.9244555867439335], "isController": false}, {"data": ["准备登录测试用户", 500, 0, 0.0, 17.89799999999998, 0, 1754, 0.0, 1.0, 1.0, 982.8900000000019, 29.256875365710943, 0.6857080163838503, 0.0], "isController": false}, {"data": ["生成注册数据", 500, 0, 0.0, 19.449999999999996, 0, 1838, 0.0, 1.0, 1.0, 1047.0100000000018, 33.73136342170951, 1.343984011333738, 0.0], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 308, 61.6, 201.4939999999998, 5, 1010, 166.0, 456.0, 520.95, 692.950000000001, 30.848963474827247, 21.155520229207802, 9.82105673124383], "isController": false}, {"data": ["登出清理", 500, 500, 100.0, 5.460000000000003, 2, 26, 4.0, 8.900000000000034, 12.0, 20.99000000000001, 32.44646333549643, 32.40527153634004, 14.39811810512654], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 387, 27.901946647440518, 4.348314606741573], "isController": false}, {"data": ["403", 1000, 72.09805335255948, 11.235955056179776], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 8900, 1387, "403", 1000, "500", 387, "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["登出清理-1", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["并发创建预约", 100, 79, "500", 79, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 308, "500", 308, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["登出清理", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
