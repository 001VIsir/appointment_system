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

    var data = {"OkPercent": 53.93258426966292, "KoPercent": 46.06741573033708};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.4799438202247191, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "登出清理-1"], "isController": false}, {"data": [1.0, 500, 1500, "登出清理-0"], "isController": false}, {"data": [0.807, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.5, 500, 1500, "准备并发测试数据"], "isController": false}, {"data": [0.802, 500, 1500, "注册测试用户"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tasks/{id}"], "isController": false}, {"data": [0.0, 500, 1500, "并发创建预约"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tasks/{id}/slots"], "isController": false}, {"data": [0.802, 500, 1500, "POST /api/auth/register"], "isController": false}, {"data": [0.991, 500, 1500, "准备预约测试数据"], "isController": false}, {"data": [0.8458333333333333, 500, 1500, "注册用户"], "isController": false}, {"data": [0.0, 500, 1500, "查询任务"], "isController": false}, {"data": [0.87, 500, 1500, "登录"], "isController": false}, {"data": [0.0, 500, 1500, "查询可用时段"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tasks/{id}/slots/available"], "isController": false}, {"data": [0.991, 500, 1500, "准备登录测试用户"], "isController": false}, {"data": [0.991, 500, 1500, "生成注册数据"], "isController": false}, {"data": [0.0, 500, 1500, "POST /api/bookings (创建预约)"], "isController": false}, {"data": [0.0, 500, 1500, "登出清理"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 8900, 4100, 46.06741573033708, 231.80123595505648, 0, 1786, 149.0, 558.9000000000005, 727.0, 973.9899999999998, 456.3166529942576, 224.67005966724773, 109.45120632306192], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["登出清理-1", 500, 500, 100.0, 2.7160000000000006, 1, 20, 2.0, 4.0, 6.0, 10.990000000000009, 31.88978888959755, 13.453504687798967, 5.792481185024555], "isController": false}, {"data": ["登出清理-0", 500, 0, 0.0, 3.556000000000001, 0, 46, 3.0, 6.0, 8.0, 18.0, 31.87149413564508, 18.38524568938042, 8.353817408210096], "isController": false}, {"data": ["POST /api/auth/login", 500, 0, 0.0, 473.3940000000001, 97, 1389, 459.0, 731.0, 840.8, 1015.96, 31.480198954857396, 22.502009322073917, 8.816915097903419], "isController": false}, {"data": ["准备并发测试数据", 100, 0, 0.0, 728.0900000000001, 708, 739, 732.0, 737.0, 737.0, 738.99, 134.9527665317139, 3.8219045209176787, 0.0], "isController": false}, {"data": ["注册测试用户", 500, 0, 0.0, 474.00199999999967, 96, 1602, 468.0, 739.0000000000003, 903.2999999999998, 1164.6400000000003, 31.440608690184245, 19.82717244780859, 13.04908075520342], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 500, 100.0, 275.2580000000001, 3, 1349, 304.5, 479.0, 684.0, 875.96, 28.80682145532062, 10.690031399435387, 5.204357391830386], "isController": false}, {"data": ["并发创建预约", 100, 100, 100.0, 330.5, 51, 647, 328.0, 400.9, 463.8999999999995, 645.2099999999991, 154.5595054095827, 88.281551874034, 50.111089644513136], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 500, 100.0, 283.6040000000005, 3, 1731, 304.0, 519.2000000000013, 706.55, 914.98, 28.885037550548816, 16.63969977614096, 5.387736496244946], "isController": false}, {"data": ["POST /api/auth/register", 500, 0, 0.0, 475.7499999999999, 95, 1217, 470.0, 756.8000000000001, 829.8, 1078.4900000000005, 38.79878947776829, 24.420427926398695, 13.814490862885078], "isController": false}, {"data": ["准备预约测试数据", 500, 0, 0.0, 17.360000000000024, 0, 1746, 0.0, 1.0, 1.0, 942.0300000000018, 25.912106135986733, 0.6579245698590381, 0.0], "isController": false}, {"data": ["注册用户", 600, 0, 0.0, 419.8883333333329, 74, 1460, 400.5, 775.8, 874.0, 1068.91, 34.002040122407344, 21.40069810438626, 13.740277541652498], "isController": false}, {"data": ["查询任务", 500, 500, 100.0, 208.15599999999978, 4, 1308, 148.5, 445.90000000000003, 598.4999999999999, 879.7600000000002, 30.131372785344098, 11.181564119561287, 7.385717352657586], "isController": false}, {"data": ["登录", 600, 0, 0.0, 374.2650000000002, 71, 1445, 359.5, 645.2999999999998, 780.8999999999999, 999.8800000000001, 34.27983774210135, 21.90115623036051, 11.236355017711249], "isController": false}, {"data": ["查询可用时段", 500, 500, 100.0, 228.46800000000016, 5, 1345, 190.0, 462.7000000000001, 658.0, 913.99, 30.374825344754267, 17.794130349766114, 7.91999840532167], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 500, 100.0, 258.8960000000002, 4, 1334, 276.0, 455.90000000000003, 649.0, 928.8800000000001, 28.775322283609576, 16.857842714088395, 5.648281034185082], "isController": false}, {"data": ["准备登录测试用户", 500, 0, 0.0, 18.05599999999998, 0, 1768, 0.0, 1.0, 1.0, 974.850000000002, 28.640164967350213, 0.6712538664222707, 0.0], "isController": false}, {"data": ["生成注册数据", 500, 0, 0.0, 18.608000000000015, 0, 1786, 0.0, 1.0, 2.0, 998.0100000000018, 34.88940060009769, 1.3901245551601424, 0.0], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 500, 100.0, 217.01599999999968, 5, 936, 184.5, 452.90000000000003, 497.4499999999999, 866.8900000000001, 30.38774766014343, 17.357517074115716, 9.674224352740975], "isController": false}, {"data": ["登出清理", 500, 500, 100.0, 6.520000000000002, 2, 48, 6.0, 10.900000000000034, 13.949999999999989, 25.980000000000018, 31.869462680859204, 31.82900340206514, 14.14207406463127], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 2100, 51.21951219512195, 23.59550561797753], "isController": false}, {"data": ["403", 1000, 24.390243902439025, 11.235955056179776], "isController": false}, {"data": ["404", 1000, 24.390243902439025, 11.235955056179776], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 8900, 4100, "500", 2100, "403", 1000, "404", 1000, "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["登出清理-1", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 500, "404", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["并发创建预约", 100, 100, "500", 100, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 500, "500", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["查询任务", 500, 500, "404", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": ["查询可用时段", 500, 500, "500", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 500, "500", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 500, "500", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["登出清理", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
