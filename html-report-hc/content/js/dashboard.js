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

    var data = {"OkPercent": 86.84269662921348, "KoPercent": 13.157303370786517};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.7467415730337079, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "登出清理-1"], "isController": false}, {"data": [1.0, 500, 1500, "登出清理-0"], "isController": false}, {"data": [0.684, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.5, 500, 1500, "准备并发测试数据"], "isController": false}, {"data": [0.667, 500, 1500, "注册测试用户"], "isController": false}, {"data": [0.879, 500, 1500, "GET /api/tasks/{id}"], "isController": false}, {"data": [0.205, 500, 1500, "并发创建预约"], "isController": false}, {"data": [0.897, 500, 1500, "GET /api/tasks/{id}/slots"], "isController": false}, {"data": [0.626, 500, 1500, "POST /api/auth/register"], "isController": false}, {"data": [0.991, 500, 1500, "准备预约测试数据"], "isController": false}, {"data": [0.795, 500, 1500, "注册用户"], "isController": false}, {"data": [0.922, 500, 1500, "查询任务"], "isController": false}, {"data": [0.81, 500, 1500, "登录"], "isController": false}, {"data": [0.928, 500, 1500, "查询可用时段"], "isController": false}, {"data": [0.9, 500, 1500, "GET /api/tasks/{id}/slots/available"], "isController": false}, {"data": [0.991, 500, 1500, "准备登录测试用户"], "isController": false}, {"data": [0.989, 500, 1500, "生成注册数据"], "isController": false}, {"data": [0.751, 500, 1500, "POST /api/bookings (创建预约)"], "isController": false}, {"data": [0.0, 500, 1500, "登出清理"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 8900, 1171, 13.157303370786517, 271.2144943820227, 0, 2024, 169.0, 648.0, 838.0, 1127.9799999999996, 425.1456959969428, 240.73054003057226, 101.9746024708608], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["登出清理-1", 500, 500, 100.0, 2.4860000000000015, 1, 34, 2.0, 4.0, 5.0, 13.0, 29.97781641585227, 12.646891300437677, 5.445189309910667], "isController": false}, {"data": ["登出清理-0", 500, 0, 0.0, 3.410000000000002, 0, 27, 3.0, 6.0, 9.0, 16.0, 29.970628783791884, 17.28872111580651, 7.855582778876701], "isController": false}, {"data": ["POST /api/auth/login", 500, 0, 0.0, 543.9019999999998, 96, 1580, 545.0, 863.2000000000003, 1007.6499999999999, 1368.3000000000015, 29.631385563588953, 21.19917291395046, 8.299102909802063], "isController": false}, {"data": ["准备并发测试数据", 100, 0, 0.0, 828.5400000000001, 823, 845, 826.0, 838.0, 840.0, 844.96, 117.9245283018868, 3.3396594929245285, 0.0], "isController": false}, {"data": ["注册测试用户", 500, 0, 0.0, 558.6659999999997, 102, 1678, 560.0, 873.1000000000004, 1007.75, 1189.94, 29.62260797440607, 18.697998437407428, 12.294539442502517], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 0, 0.0, 326.0860000000001, 7, 1601, 358.0, 555.8000000000001, 781.55, 1251.770000000002, 27.898672023211695, 21.55597432624707, 5.040287426068519], "isController": false}, {"data": ["并发创建预约", 100, 72, 72.0, 722.8600000000002, 177, 988, 751.5, 906.0, 908.0, 987.3599999999997, 98.81422924901186, 64.90377192440711, 32.03742588932806], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 0, 0.0, 311.2259999999998, 4, 2024, 310.5, 535.9000000000001, 769.8, 1062.95, 27.976723366159355, 18.63900016436325, 5.218314612242614], "isController": false}, {"data": ["POST /api/auth/register", 500, 0, 0.0, 608.6880000000001, 98, 1762, 595.5, 973.2000000000003, 1081.95, 1377.4600000000014, 36.64883090229422, 23.097782516675217, 13.048988034156709], "isController": false}, {"data": ["准备预约测试数据", 500, 0, 0.0, 19.137999999999977, 0, 1833, 0.0, 1.0, 1.0, 1026.0400000000018, 24.172105390379503, 0.6137448634276046, 0.0], "isController": false}, {"data": ["注册用户", 600, 0, 0.0, 454.548333333333, 81, 1593, 395.5, 870.9, 1034.8999999999999, 1125.98, 31.69739553066723, 19.967192370172754, 12.808967061123145], "isController": false}, {"data": ["查询任务", 500, 0, 0.0, 259.5139999999999, 8, 1327, 171.0, 535.9000000000001, 766.9, 1056.98, 28.16266756787203, 21.773977431142278, 6.9031538667342565], "isController": false}, {"data": ["登录", 600, 0, 0.0, 423.6649999999997, 75, 1638, 384.5, 760.6999999999999, 941.7499999999997, 1201.7600000000002, 31.891144892101625, 20.392892261082174, 10.453381624056554], "isController": false}, {"data": ["查询可用时段", 500, 0, 0.0, 248.0099999999999, 7, 1326, 174.0, 527.9000000000001, 559.8499999999999, 1054.93, 29.420417769932335, 19.615661315828184, 7.671144086496028], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 0, 0.0, 295.34200000000016, 4, 1676, 279.5, 531.9000000000001, 645.75, 1163.3000000000006, 27.861361863367883, 18.56333975885991, 5.468880600133734], "isController": false}, {"data": ["准备登录测试用户", 500, 0, 0.0, 19.95200000000001, 0, 1872, 0.0, 1.0, 1.0, 1070.0100000000018, 26.983270372369134, 0.6324203993524015, 0.0], "isController": false}, {"data": ["生成注册数据", 500, 0, 0.0, 20.754000000000023, 0, 1901, 0.0, 1.0, 2.0, 1109.0200000000018, 33.07534563736191, 1.3178458027386386, 0.0], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 99, 19.8, 240.1440000000001, 15, 1253, 173.0, 532.4000000000002, 564.8, 1022.2100000000007, 29.40311673037342, 23.832776664951485, 9.360757865333726], "isController": false}, {"data": ["登出清理", 500, 500, 100.0, 6.163999999999995, 2, 37, 5.0, 10.0, 13.949999999999989, 25.960000000000036, 29.967036260113876, 29.92899217111178, 13.297872340425533], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 171, 14.602903501280956, 1.9213483146067416], "isController": false}, {"data": ["403", 1000, 85.39709649871904, 11.235955056179776], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 8900, 1171, "403", 1000, "500", 171, "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["登出清理-1", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["并发创建预约", 100, 72, "500", 72, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 99, "500", 99, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["登出清理", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
