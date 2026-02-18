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

    var data = {"OkPercent": 20.253164556962027, "KoPercent": 79.74683544303798};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.20158227848101265, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [1.0, 500, 1500, "准备并发测试数据"], "isController": false}, {"data": [0.0, 500, 1500, "注册测试用户"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tasks/{id}"], "isController": false}, {"data": [0.0, 500, 1500, "并发创建预约"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tasks/{id}/slots"], "isController": false}, {"data": [0.0, 500, 1500, "POST /api/auth/register"], "isController": false}, {"data": [0.995, 500, 1500, "准备预约测试数据"], "isController": false}, {"data": [0.0, 500, 1500, "注册用户"], "isController": false}, {"data": [0.0, 500, 1500, "查询任务"], "isController": false}, {"data": [0.0, 500, 1500, "登录"], "isController": false}, {"data": [0.0, 500, 1500, "查询可用时段"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tasks/{id}/slots/available"], "isController": false}, {"data": [0.995, 500, 1500, "准备登录测试用户"], "isController": false}, {"data": [0.995, 500, 1500, "生成注册数据"], "isController": false}, {"data": [0.0, 500, 1500, "POST /api/bookings (创建预约)"], "isController": false}, {"data": [0.0, 500, 1500, "登出清理"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 7900, 6300, 79.74683544303798, 5.957974683544305, 0, 1376, 0.0, 0.0, 1.0, 305.9899999999998, 801.8676410881039, 905.2829533343483, 0.0], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["POST /api/auth/login", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 59.0388475616956, 83.13868963277838, 0.0], "isController": false}, {"data": ["准备并发测试数据", 100, 0, 0.0, 308.65, 294, 331, 309.0, 321.0, 323.0, 330.99, 301.20481927710847, 8.530214608433734, 0.0], "isController": false}, {"data": ["注册测试用户", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 59.031877213695395, 83.12887396694214, 0.0], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 52.39991616013415, 73.7897256864389, 0.0], "isController": false}, {"data": ["并发创建预约", 100, 100, 100.0, 0.010000000000000002, 0, 1, 0.0, 0.0, 0.0, 0.9899999999999949, 100000.0, 140820.3125, 0.0], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 52.42189138184106, 73.82067126231914, 0.0], "isController": false}, {"data": ["POST /api/auth/register", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 59.31198102016608, 83.52331702253855, 0.0], "isController": false}, {"data": ["准备预约测试数据", 500, 0, 0.0, 10.290000000000003, 0, 1310, 0.0, 1.0, 1.0, 506.98000000000184, 50.9683995922528, 1.2941195208970437, 0.0], "isController": false}, {"data": ["注册用户", 600, 600, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 70.54673721340387, 99.34413580246913, 0.0], "isController": false}, {"data": ["查询任务", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 58.79586077140169, 82.79651487535278, 0.0], "isController": false}, {"data": ["登录", 600, 600, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 70.55503292568203, 99.35581785042334, 0.0], "isController": false}, {"data": ["查询可用时段", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 58.802775491003175, 82.80625220510407, 0.0], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 52.42738806752648, 73.8284117122785, 0.0], "isController": false}, {"data": ["准备登录测试用户", 500, 0, 0.0, 10.760000000000002, 0, 1351, 0.0, 1.0, 1.0, 543.9600000000019, 50.94243504839532, 1.1939633214467653, 0.0], "isController": false}, {"data": ["生成注册数据", 500, 0, 0.0, 11.353999999999981, 0, 1376, 0.0, 1.0, 2.0, 585.9800000000018, 50.994390617032124, 2.0318077511473738, 0.0], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 58.80969183721477, 82.81599182545283, 0.0], "isController": false}, {"data": ["登出清理", 500, 500, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 59.031877213695395, 83.12887396694214, 0.0], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 6300, 100.0, 79.74683544303798], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 7900, 6300, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 6300, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["POST /api/auth/login", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": ["注册测试用户", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["并发创建预约", 100, 100, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 100, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/auth/register", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": ["注册用户", 600, 600, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 600, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["查询任务", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["登录", 600, 600, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 600, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["查询可用时段", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["登出清理", 500, 500, "Non HTTP response code: java.lang.IllegalArgumentException/Non HTTP response message: Host may not be blank", 500, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
