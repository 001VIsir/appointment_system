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

    var data = {"OkPercent": 82.02247191011236, "KoPercent": 17.97752808988764};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.7657865168539326, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "登出清理-1"], "isController": false}, {"data": [1.0, 500, 1500, "登出清理-0"], "isController": false}, {"data": [0.859, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.5, 500, 1500, "准备并发测试数据"], "isController": false}, {"data": [0.859, 500, 1500, "注册测试用户"], "isController": false}, {"data": [0.964, 500, 1500, "GET /api/tasks/{id}"], "isController": false}, {"data": [0.0, 500, 1500, "并发创建预约"], "isController": false}, {"data": [0.972, 500, 1500, "GET /api/tasks/{id}/slots"], "isController": false}, {"data": [0.847, 500, 1500, "POST /api/auth/register"], "isController": false}, {"data": [0.993, 500, 1500, "准备预约测试数据"], "isController": false}, {"data": [0.8791666666666667, 500, 1500, "注册用户"], "isController": false}, {"data": [0.976, 500, 1500, "查询任务"], "isController": false}, {"data": [0.8933333333333333, 500, 1500, "登录"], "isController": false}, {"data": [0.976, 500, 1500, "查询可用时段"], "isController": false}, {"data": [0.972, 500, 1500, "GET /api/tasks/{id}/slots/available"], "isController": false}, {"data": [0.993, 500, 1500, "准备登录测试用户"], "isController": false}, {"data": [0.993, 500, 1500, "生成注册数据"], "isController": false}, {"data": [0.0, 500, 1500, "POST /api/bookings (创建预约)"], "isController": false}, {"data": [0.0, 500, 1500, "登出清理"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 8900, 1600, 17.97752808988764, 217.18516853932533, 0, 1682, 156.0, 518.0, 605.9499999999989, 893.9799999999996, 465.7491234496834, 250.62179265987228, 111.71365995734993], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["登出清理-1", 500, 500, 100.0, 2.1619999999999995, 1, 19, 2.0, 4.0, 5.0, 8.990000000000009, 32.87959492339054, 13.871079108305384, 5.972270171631485], "isController": false}, {"data": ["登出清理-0", 500, 0, 0.0, 3.2439999999999984, 0, 58, 2.0, 6.0, 7.0, 18.980000000000018, 32.83856561145409, 18.94310615887298, 8.607295908314724], "isController": false}, {"data": ["POST /api/auth/login", 500, 0, 0.0, 426.53799999999984, 80, 1439, 432.5, 574.6000000000001, 747.3499999999999, 987.8500000000001, 32.32479958624256, 23.126121266485647, 9.053469259115593], "isController": false}, {"data": ["准备并发测试数据", 100, 0, 0.0, 601.3800000000001, 589, 626, 598.0, 620.0, 622.0, 625.98, 159.4896331738437, 4.51679625199362, 0.0], "isController": false}, {"data": ["注册测试用户", 500, 0, 0.0, 425.7579999999998, 97, 1338, 438.5, 635.0000000000003, 790.0, 1005.94, 32.615786040443574, 20.587568289302023, 13.536825260926289], "isController": false}, {"data": ["GET /api/tasks/{id}", 500, 0, 0.0, 267.0060000000001, 6, 1158, 271.5, 460.7000000000001, 630.8499999999999, 816.99, 30.178657653307578, 23.34130552873008, 5.452198892443263], "isController": false}, {"data": ["并发创建预约", 100, 100, 100.0, 424.99000000000007, 309, 706, 418.5, 438.9, 679.5499999999972, 705.97, 136.6120218579235, 78.02894467213115, 44.29217896174863], "isController": false}, {"data": ["GET /api/tasks/{id}/slots", 500, 0, 0.0, 256.0399999999998, 3, 1444, 271.0, 445.80000000000007, 523.75, 898.9300000000001, 30.391441769997567, 20.241175085096035, 5.66871618952103], "isController": false}, {"data": ["POST /api/auth/register", 500, 0, 0.0, 449.72999999999996, 93, 1398, 449.5, 673.8000000000001, 755.95, 996.9300000000001, 39.425958050780636, 24.85021216093676, 14.037797173158808], "isController": false}, {"data": ["准备预约测试数据", 500, 0, 0.0, 15.239999999999988, 0, 1604, 0.0, 1.0, 1.0, 834.7200000000021, 26.488662852299218, 0.6725637052341598, 0.0], "isController": false}, {"data": ["注册用户", 600, 0, 0.0, 377.0616666666666, 81, 1044, 351.0, 613.8, 748.8499999999998, 976.8600000000001, 34.57615397913905, 21.782076586181063, 13.972277848210682], "isController": false}, {"data": ["查询任务", 500, 0, 0.0, 215.88200000000003, 8, 900, 177.0, 433.7000000000001, 486.84999999999997, 812.8100000000002, 30.11866755014758, 23.29490693331727, 7.382603081139691], "isController": false}, {"data": ["登录", 600, 0, 0.0, 368.25000000000017, 75, 1072, 335.5, 614.5999999999999, 792.0, 1004.6000000000004, 34.638032559750606, 22.149398164184277, 11.353765262383098], "isController": false}, {"data": ["查询可用时段", 500, 0, 0.0, 210.91400000000004, 5, 1304, 179.5, 423.90000000000003, 476.69999999999993, 842.8800000000001, 30.167732593218293, 16.409596732834558, 7.866000588270785], "isController": false}, {"data": ["GET /api/tasks/{id}/slots/available", 500, 0, 0.0, 252.03999999999976, 4, 923, 256.0, 440.90000000000003, 609.5999999999999, 809.96, 30.248033877797944, 16.453276240169387, 5.937358212341198], "isController": false}, {"data": ["准备登录测试用户", 500, 0, 0.0, 16.06599999999999, 0, 1659, 0.0, 1.0, 1.0, 865.8700000000019, 29.66830831305999, 0.6953509760873434, 0.0], "isController": false}, {"data": ["生成注册数据", 500, 0, 0.0, 16.753999999999976, 0, 1682, 0.0, 1.0, 1.0, 892.0100000000018, 35.62776115148924, 1.4195436083796493, 0.0], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 500, 100.0, 203.13600000000002, 5, 1203, 163.5, 406.0, 464.0, 913.5700000000004, 30.380362133916634, 17.35306110630089, 9.671873101227368], "isController": false}, {"data": ["登出清理", 500, 500, 100.0, 5.737999999999999, 2, 60, 4.0, 9.0, 11.0, 27.960000000000036, 32.834252692408725, 32.792568582545314, 14.57019963225637], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 600, 37.5, 6.741573033707865], "isController": false}, {"data": ["403", 1000, 62.5, 11.235955056179776], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 8900, 1600, "403", 1000, "500", 600, "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["登出清理-1", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["并发创建预约", 100, 100, "500", 100, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/bookings (创建预约)", 500, 500, "500", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["登出清理", 500, 500, "403", 500, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
