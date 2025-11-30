import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/**
 * Custom date adapter that supports dd/MM/yyyy format for Vietnamese users
 */
@Injectable()
export class CustomDateAdapter extends NativeDateAdapter {

  /**
   * Parse date string in dd/MM/yyyy format
   */
  override parse(value: any): Date | null {
    if (!value || typeof value !== 'string') {
      return null;
    }

    // Remove extra spaces
    value = value.trim();

    // Try to parse dd/MM/yyyy or d/M/yyyy format
    const ddMMyyyyRegex = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;
    const match = value.match(ddMMyyyyRegex);

    if (match) {
      const day = parseInt(match[1], 10);
      const month = parseInt(match[2], 10) - 1; // Month is 0-indexed
      const year = parseInt(match[3], 10);

      // Validate date parts
      if (day >= 1 && day <= 31 && month >= 0 && month <= 11 && year >= 1 && year <= 9999) {
        const date = new Date(year, month, day);

        // Check if the date is valid (e.g., not Feb 30)
        if (date.getDate() === day && date.getMonth() === month && date.getFullYear() === year) {
          return date;
        }
      }
    }

    // Try to parse dd-MM-yyyy format as well
    const ddMMyyyyDashRegex = /^(\d{1,2})-(\d{1,2})-(\d{4})$/;
    const dashMatch = value.match(ddMMyyyyDashRegex);

    if (dashMatch) {
      const day = parseInt(dashMatch[1], 10);
      const month = parseInt(dashMatch[2], 10) - 1;
      const year = parseInt(dashMatch[3], 10);

      if (day >= 1 && day <= 31 && month >= 0 && month <= 11 && year >= 1 && year <= 9999) {
        const date = new Date(year, month, day);

        if (date.getDate() === day && date.getMonth() === month && date.getFullYear() === year) {
          return date;
        }
      }
    }

    // Fall back to native parsing
    return super.parse(value);
  }

  /**
   * Format date as dd/MM/yyyy
   */
  override format(date: Date, displayFormat: Object): string {
    if (!this.isValid(date)) {
      return '';
    }

    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();

    return `${day}/${month}/${year}`;
  }
}
