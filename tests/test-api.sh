#!/bin/bash

# Appointment System API Test Script - Improved Version
# Tests all API endpoints with better session handling

BASE_URL="http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function to print test results
print_result() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$2" = "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}[PASS]${NC} $1"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}[FAIL]${NC} $1 - $3"
    fi
}

# Helper to make requests and save cookies
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local cookie_file=$4

    if [ -z "$cookie_file" ]; then
        cookie_file="/tmp/cookies.txt"
    fi

    if [ "$method" = "GET" ]; then
        curl -s -w "\n%{http_code}" -c "$cookie_file" -b "$cookie_file" "$url"
    elif [ "$method" = "POST" ]; then
        if [ -z "$data" ]; then
            curl -s -w "\n%{http_code}" -c "$cookie_file" -b "$cookie_file" -X POST "$url"
        else
            curl -s -w "\n%{http_code}" -c "$cookie_file" -b "$cookie_file" -X POST "$url" -H "Content-Type: application/json" -d "$data"
        fi
    elif [ "$method" = "PUT" ]; then
        curl -s -w "\n%{http_code}" -c "$cookie_file" -b "$cookie_file" -X PUT "$url" -H "Content-Type: application/json" -d "$data"
    elif [ "$method" = "DELETE" ]; then
        curl -s -w "\n%{http_code}" -c "$cookie_file" -b "$cookie_file" -X DELETE "$url"
    fi
}

# Login function
do_login() {
    local username=$1
    local password=$2
    local cookie_file=$3
    make_request "POST" "$BASE_URL/api/auth/login" "{\"username\":\"$username\",\"password\":\"$password\"}" "$cookie_file"
}

echo "========================================="
echo "Appointment System API Test Suite"
echo "========================================="
echo ""

# ============================================
# Phase 1: Public Endpoints
# ============================================
echo "=== Phase 1: Public Endpoints ==="

# Test 1.1: Health check
echo -e "${YELLOW}Test 1.1: Health check${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Health check" "PASS"
else
    print_result "Health check" "FAIL" "HTTP $code"
fi

# Test 1.2: Swagger UI
echo -e "${YELLOW}Test 1.2: Swagger UI accessible${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/swagger-ui.html")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Swagger UI accessible" "PASS"
else
    print_result "Swagger UI accessible" "FAIL" "HTTP $code"
fi

# Test 1.3: OpenAPI docs
echo -e "${YELLOW}Test 1.3: OpenAPI docs accessible${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api-docs")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "OpenAPI docs accessible" "PASS"
else
    print_result "OpenAPI docs accessible" "FAIL" "HTTP $code"
fi

# Test 1.4: Public task endpoint
echo -e "${YELLOW}Test 1.4: Public task endpoint accessible${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasks/1")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ] || [ "$code" = "404" ]; then
    print_result "Public task endpoint accessible" "PASS"
else
    print_result "Public task endpoint accessible" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 2: Authentication
# ============================================
echo ""
echo "=== Phase 2: Authentication ==="

# Create unique test users with timestamp
TIMESTAMP=$(date +%s)
USER_FILE="/tmp/user_creds_${TIMESTAMP}.txt"

# Test 2.1: Register a new user
echo -e "${YELLOW}Test 2.1: Register new user${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"testuser${TIMESTAMP}\",\"password\":\"password123\",\"email\":\"testuser${TIMESTAMP}@example.com\",\"role\":\"USER\"}")
code=$(echo "$response" | tail -1)
if [ "$code" = "201" ] || [ "$code" = "200" ]; then
    print_result "Register new user" "PASS"
    echo "testuser${TIMESTAMP}" > "$USER_FILE"
else
    print_result "Register new user" "FAIL" "HTTP $code"
    echo "testuser${TIMESTAMP}" > "$USER_FILE"
fi

# Test 2.2: Register a merchant
echo -e "${YELLOW}Test 2.2: Register merchant${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"merchant${TIMESTAMP}\",\"password\":\"password123\",\"email\":\"merchant${TIMESTAMP}@example.com\",\"role\":\"MERCHANT\"}")
code=$(echo "$response" | tail -1)
if [ "$code" = "201" ] || [ "$code" = "200" ]; then
    print_result "Register merchant" "PASS"
else
    print_result "Register merchant" "FAIL" "HTTP $code"
fi

# Test 2.3: Login as user
echo -e "${YELLOW}Test 2.3: Login as user${NC}"
USER_COOKIE="/tmp/user_cookie_${TIMESTAMP}.txt"
response=$(curl -s -w "\n%{http_code}" -c "$USER_COOKIE" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"testuser${TIMESTAMP}\",\"password\":\"password123\"}")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Login as user" "PASS"
else
    print_result "Login as user" "FAIL" "HTTP $code"
fi

# Test 2.4: Get current user info
echo -e "${YELLOW}Test 2.4: Get current user info${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" "$BASE_URL/api/auth/me")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get current user info" "PASS"
else
    print_result "Get current user info" "FAIL" "HTTP $code"
fi

# Test 2.5: Login with wrong password
echo -e "${YELLOW}Test 2.5: Login with wrong password${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"testuser${TIMESTAMP}\",\"password\":\"wrongpass\"}")
code=$(echo "$response" | tail -1)
if [ "$code" = "401" ]; then
    print_result "Login with wrong password (401)" "PASS"
else
    print_result "Login with wrong password" "FAIL" "HTTP $code"
fi

# Test 2.6: Logout
echo -e "${YELLOW}Test 2.6: Logout${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" -X POST "$BASE_URL/api/auth/logout")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Logout" "PASS"
else
    print_result "Logout" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 3: Merchant Operations
# ============================================
echo ""
echo "=== Phase 3: Merchant Operations ==="

# Login as merchant
MERCHANT_COOKIE="/tmp/merchant_cookie_${TIMESTAMP}.txt"
curl -s -c "$MERCHANT_COOKIE" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"merchant${TIMESTAMP}\",\"password\":\"password123\"}" > /dev/null

# Test 3.1: Create merchant profile
echo -e "${YELLOW}Test 3.1: Create merchant profile${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X POST "$BASE_URL/api/merchants/profile" \
    -H "Content-Type: application/json" \
    -d '{"businessName":"Test Medical Clinic","description":"A professional medical clinic","phone":"1234567890","address":"123 Medical St"}')
code=$(echo "$response" | tail -1)
if [ "$code" = "201" ]; then
    print_result "Create merchant profile" "PASS"
else
    print_result "Create merchant profile" "FAIL" "HTTP $code"
fi

# Test 3.2: Get merchant profile
echo -e "${YELLOW}Test 3.2: Get merchant profile${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/profile")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get merchant profile" "PASS"
else
    print_result "Get merchant profile" "FAIL" "HTTP $code"
fi

# Test 3.3: Update merchant profile
echo -e "${YELLOW}Test 3.3: Update merchant profile${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X PUT "$BASE_URL/api/merchants/profile" \
    -H "Content-Type: application/json" \
    -d '{"businessName":"Updated Medical Clinic","description":"Updated description","phone":"9876543210","address":"456 Health Ave"}')
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Update merchant profile" "PASS"
else
    print_result "Update merchant profile" "FAIL" "HTTP $code"
fi

# Test 3.4: Get merchant settings
echo -e "${YELLOW}Test 3.4: Get merchant settings${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/settings")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get merchant settings" "PASS"
else
    print_result "Get merchant settings" "FAIL" "HTTP $code"
fi

# Test 3.5: Update merchant settings
echo -e "${YELLOW}Test 3.5: Update merchant settings${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X PUT "$BASE_URL/api/merchants/settings" \
    -H "Content-Type: application/json" \
    -d '{"notificationsEnabled":true,"timezone":"Asia/Shanghai","bookingAdvanceDays":30,"cancelDeadlineHours":24,"autoConfirmBookings":false,"maxBookingsPerUserPerDay":3}')
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Update merchant settings" "PASS"
else
    print_result "Update merchant settings" "FAIL" "HTTP $code"
fi

# Test 3.6: Check profile exists
echo -e "${YELLOW}Test 3.6: Check profile exists${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/profile/exists")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Check profile exists" "PASS"
else
    print_result "Check profile exists" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 4: Service Items
# ============================================
echo ""
echo "=== Phase 4: Service Items ==="

# Test 4.1: Create service item
echo -e "${YELLOW}Test 4.1: Create service item${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X POST "$BASE_URL/api/merchants/services" \
    -H "Content-Type: application/json" \
    -d '{"name":"General Checkup","description":"Basic health checkup","category":"MEDICAL","duration":30,"price":100.00}')
code=$(echo "$response" | tail -1)
SERVICE_ID=$(echo "$response" | head -1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ "$code" = "201" ]; then
    print_result "Create service item" "PASS"
else
    print_result "Create service item" "FAIL" "HTTP $code"
fi

# Test 4.2: Get all service items
echo -e "${YELLOW}Test 4.2: Get all service items${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/services")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get all service items" "PASS"
else
    print_result "Get all service items" "FAIL" "HTTP $code"
fi

# Test 4.3: Get active service items
echo -e "${YELLOW}Test 4.3: Get active service items${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/services/active")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get active service items" "PASS"
else
    print_result "Get active service items" "FAIL" "HTTP $code"
fi

# Test 4.4: Count service items
echo -e "${YELLOW}Test 4.4: Count service items${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/services/count")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Count service items" "PASS"
else
    print_result "Count service items" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 5: Appointment Tasks
# ============================================
echo ""
echo "=== Phase 5: Appointment Tasks ==="

# Use default service ID if not found
if [ -z "$SERVICE_ID" ]; then
    SERVICE_ID=1
fi

TOMORROW=$(date -d "tomorrow" +%Y-%m-%d 2>/dev/null || date -v+1d +%Y-%m-%d 2>/dev/null || echo "2026-02-19")

# Test 5.1: Create appointment task
echo -e "${YELLOW}Test 5.1: Create appointment task${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X POST "$BASE_URL/api/merchants/tasks" \
    -H "Content-Type: application/json" \
    -d "{\"serviceId\":$SERVICE_ID,\"title\":\"Morning Checkup\",\"description\":\"Morning health checkup\",\"taskDate\":\"$TOMORROW\",\"totalCapacity\":10}")
code=$(echo "$response" | tail -1)
TASK_ID=$(echo "$response" | head -1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ "$code" = "201" ]; then
    print_result "Create appointment task" "PASS"
else
    print_result "Create appointment task" "FAIL" "HTTP $code"
fi

# Test 5.2: Get all tasks
echo -e "${YELLOW}Test 5.2: Get all tasks${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/tasks")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get all tasks" "PASS"
else
    print_result "Get all tasks" "FAIL" "HTTP $code"
fi

# Test 5.3: Get active tasks
echo -e "${YELLOW}Test 5.3: Get active tasks${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/tasks/active")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get active tasks" "PASS"
else
    print_result "Get active tasks" "FAIL" "HTTP $code"
fi

# Use default task ID if not found
if [ -z "$TASK_ID" ]; then
    TASK_ID=1
fi

# Test 5.4: Get task by ID
echo -e "${YELLOW}Test 5.4: Get task by ID${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/tasks/$TASK_ID")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get task by ID" "PASS"
else
    print_result "Get task by ID" "FAIL" "HTTP $code"
fi

# Test 5.5: Update task
echo -e "${YELLOW}Test 5.5: Update task${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X PUT "$BASE_URL/api/merchants/tasks/$TASK_ID" \
    -H "Content-Type: application/json" \
    -d "{\"serviceId\":$SERVICE_ID,\"title\":\"Updated Morning Checkup\",\"description\":\"Updated\",\"taskDate\":\"$TOMORROW\",\"totalCapacity\":15}")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Update task" "PASS"
else
    print_result "Update task" "FAIL" "HTTP $code"
fi

# Test 5.6: Create time slots
echo -e "${YELLOW}Test 5.6: Create time slots${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X POST "$BASE_URL/api/merchants/tasks/$TASK_ID/slots" \
    -H "Content-Type: application/json" \
    -d '[{"startTime":{"hour":9,"minute":0},"endTime":{"hour":9,"minute":30},"capacity":5},{"startTime":{"hour":10,"minute":0},"endTime":{"hour":10,"minute":30},"capacity":5}]')
code=$(echo "$response" | tail -1)
SLOT_ID=$(echo "$response" | head -1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ "$code" = "201" ]; then
    print_result "Create time slots" "PASS"
else
    print_result "Create time slots" "FAIL" "HTTP $code - Response: $(echo "$response" | head -1 | head -c 200)"
fi

# Test 5.7: Get task slots
echo -e "${YELLOW}Test 5.7: Get task slots${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/tasks/$TASK_ID/slots")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get task slots" "PASS"
else
    print_result "Get task slots" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 6: User Booking
# ============================================
echo ""
echo "=== Phase 6: User Booking ==="

# Login as user
curl -s -c "$USER_COOKIE" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"testuser${TIMESTAMP}\",\"password\":\"password123\"}" > /dev/null

# Use default slot ID
if [ -z "$SLOT_ID" ]; then
    SLOT_ID=1
fi

# Test 6.1: Get available slots (public)
echo -e "${YELLOW}Test 6.1: Get available slots (public)${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasks/$TASK_ID/slots")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get available slots (public)" "PASS"
else
    print_result "Get available slots (public)" "FAIL" "HTTP $code"
fi

# Test 6.2: Get public task
echo -e "${YELLOW}Test 6.2: Get public task${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasks/$TASK_ID")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get public task" "PASS"
else
    print_result "Get public task" "FAIL" "HTTP $code"
fi

# Test 6.3: Create booking
echo -e "${YELLOW}Test 6.3: Create booking${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" -X POST "$BASE_URL/api/bookings" \
    -H "Content-Type: application/json" \
    -d "{\"slotId\":$SLOT_ID,\"remark\":\"Regular checkup\"}")
code=$(echo "$response" | tail -1)
BOOKING_ID=$(echo "$response" | head -1 | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ "$code" = "201" ]; then
    print_result "Create booking" "PASS"
else
    print_result "Create booking" "FAIL" "HTTP $code - Response: $(echo "$response" | head -1 | head -c 200)"
fi

# Use default booking ID
if [ -z "$BOOKING_ID" ]; then
    BOOKING_ID=1
fi

# Test 6.4: Get my bookings
echo -e "${YELLOW}Test 6.4: Get my bookings${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" "$BASE_URL/api/bookings/my?page=0&size=10")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get my bookings" "PASS"
else
    print_result "Get my bookings" "FAIL" "HTTP $code"
fi

# Test 6.5: Get my active bookings
echo -e "${YELLOW}Test 6.5: Get my active bookings${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" "$BASE_URL/api/bookings/my/active")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get my active bookings" "PASS"
else
    print_result "Get my active bookings" "FAIL" "HTTP $code"
fi

# Test 6.6: Count my bookings
echo -e "${YELLOW}Test 6.6: Count my bookings${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" "$BASE_URL/api/bookings/my/count")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Count my bookings" "PASS"
else
    print_result "Count my bookings" "FAIL" "HTTP $code"
fi

# Test 6.7: Get booking by ID
echo -e "${YELLOW}Test 6.7: Get booking by ID${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$USER_COOKIE" "$BASE_URL/api/bookings/$BOOKING_ID")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get booking by ID" "PASS"
else
    print_result "Get booking by ID" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 7: Merchant Booking Management
# ============================================
echo ""
echo "=== Phase 7: Merchant Booking Management ==="

# Test 7.1: Get merchant bookings
echo -e "${YELLOW}Test 7.1: Get merchant bookings${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/bookings?page=0&size=10")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get merchant bookings" "PASS"
else
    print_result "Get merchant bookings" "FAIL" "HTTP $code"
fi

# Test 7.2: Get bookings by status
echo -e "${YELLOW}Test 7.2: Get bookings by status${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/bookings/status/PENDING")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get bookings by status" "PASS"
else
    print_result "Get bookings by status" "FAIL" "HTTP $code"
fi

# Test 7.3: Confirm booking
echo -e "${YELLOW}Test 7.3: Confirm booking${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X PUT "$BASE_URL/api/merchants/bookings/$BOOKING_ID/confirm")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Confirm booking" "PASS"
else
    print_result "Confirm booking" "FAIL" "HTTP $code"
fi

# Test 7.4: Complete booking
echo -e "${YELLOW}Test 7.4: Complete booking${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" -X PUT "$BASE_URL/api/merchants/bookings/$BOOKING_ID/complete")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Complete booking" "PASS"
else
    print_result "Complete booking" "FAIL" "HTTP $code"
fi

# Test 7.5: Get merchant stats
echo -e "${YELLOW}Test 7.5: Get merchant stats${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/stats")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get merchant stats" "PASS"
else
    print_result "Get merchant stats" "FAIL" "HTTP $code"
fi

# Test 7.6: Generate signed link
echo -e "${YELLOW}Test 7.6: Generate signed link${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/links?taskId=$TASK_ID")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Generate signed link" "PASS"
else
    print_result "Generate signed link" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 8: Admin Functions
# ============================================
echo ""
echo "=== Phase 8: Admin Functions ==="

# Register and login as admin
ADMIN_COOKIE="/tmp/admin_cookie_${TIMESTAMP}.txt"
curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"admin${TIMESTAMP}\",\"password\":\"password123\",\"email\":\"admin${TIMESTAMP}@example.com\",\"role\":\"ADMIN\"}" > /dev/null
curl -s -c "$ADMIN_COOKIE" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"admin${TIMESTAMP}\",\"password\":\"password123\"}" > /dev/null

# Test 8.1: Get user stats
echo -e "${YELLOW}Test 8.1: Get user stats${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$ADMIN_COOKIE" "$BASE_URL/api/admin/stats/users")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get user stats" "PASS"
else
    print_result "Get user stats" "FAIL" "HTTP $code"
fi

# Test 8.2: Get booking stats
echo -e "${YELLOW}Test 8.2: Get booking stats${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$ADMIN_COOKIE" "$BASE_URL/api/admin/stats/bookings")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get booking stats" "PASS"
else
    print_result "Get booking stats" "FAIL" "HTTP $code"
fi

# Test 8.3: Get dashboard stats
echo -e "${YELLOW}Test 8.3: Get dashboard stats${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$ADMIN_COOKIE" "$BASE_URL/api/admin/stats/dashboard")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get dashboard stats" "PASS"
else
    print_result "Get dashboard stats" "FAIL" "HTTP $code"
fi

# Test 8.4: Get system metrics
echo -e "${YELLOW}Test 8.4: Get system metrics${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$ADMIN_COOKIE" "$BASE_URL/api/admin/metrics")
code=$(echo "$response" | tail -1)
if [ "$code" = "200" ]; then
    print_result "Get system metrics" "PASS"
else
    print_result "Get system metrics" "FAIL" "HTTP $code"
fi

# ============================================
# Phase 9: Error Handling
# ============================================
echo ""
echo "=== Phase 9: Error Handling ==="

# Test 9.1: Access protected endpoint without auth
echo -e "${YELLOW}Test 9.1: Access protected endpoint without auth${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/auth/me")
code=$(echo "$response" | tail -1)
if [ "$code" = "401" ] || [ "$code" = "302" ]; then
    print_result "Access protected endpoint without auth" "PASS"
else
    print_result "Access protected endpoint without auth" "FAIL" "HTTP $code"
fi

# Test 9.2: Get non-existent task
echo -e "${YELLOW}Test 9.2: Get non-existent task (404)${NC}"
response=$(curl -s -w "\n%{http_code}" -b "$MERCHANT_COOKIE" "$BASE_URL/api/merchants/tasks/999999")
code=$(echo "$response" | tail -1)
if [ "$code" = "404" ]; then
    print_result "Get non-existent task" "PASS"
else
    print_result "Get non-existent task" "FAIL" "HTTP $code"
fi

# Test 9.3: Get non-existent slot
echo -e "${YELLOW}Test 9.3: Get non-existent slot (404)${NC}"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/slots/999999")
code=$(echo "$response" | tail -1)
if [ "$code" = "404" ]; then
    print_result "Get non-existent slot" "PASS"
else
    print_result "Get non-existent slot" "FAIL" "HTTP $code"
fi

# Cleanup
rm -f "/tmp/user_cookie_${TIMESTAMP}.txt" "/tmp/merchant_cookie_${TIMESTAMP}.txt" "/tmp/admin_cookie_${TIMESTAMP}.txt" "$USER_FILE"

# ============================================
# Summary
# ============================================
echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
echo -e "Total Tests: ${TOTAL_TESTS}"
echo -e "${GREEN}Passed: ${PASSED_TESTS}${NC}"
echo -e "${RED}Failed: ${FAILED_TESTS}${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi
